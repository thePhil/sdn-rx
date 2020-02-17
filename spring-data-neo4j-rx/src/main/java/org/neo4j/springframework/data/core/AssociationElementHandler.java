/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core;

import static org.neo4j.springframework.data.core.RelationshipStatementHolder.*;
import static org.neo4j.springframework.data.core.schema.CypherGenerator.*;

import java.util.Map;
import java.util.Objects;

import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.schema.CypherGenerator;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * This Handler is responsible for the persistence of a single element of an association.
 * An association can be expressed as either a single element (scalar object), a collection type (e.g. a list) or a map
 * type (Dynamic relationships or relationships with properties).
 * Independent of type of association, this class handles the persistent of a single element of such an association.
 *
 * @author Philipp Tölle
 *
 * @param <T> The type of the element
 */
public class AssociationElementHandler<T> {
	private static final Renderer renderer = Renderer.getDefaultRenderer();
	private static final CypherGenerator cypherGenerator = CypherGenerator.INSTANCE;

	private final PersistentRootNodeContext rootNodeContext;
	private final AssociationParentEntityContext<T> parentEntityContext;
	private final InverseRelationshipNodeContext inverseRelationshipNodeContext;

	public AssociationElementHandler(PersistentRootNodeContext rootNodeContext,
		AssociationParentEntityContext<T> parentEntityContext,
		InverseRelationshipNodeContext inverseRelationshipNodeContext) {
		this.rootNodeContext = Objects.requireNonNull(rootNodeContext);
		this.parentEntityContext = Objects.requireNonNull(parentEntityContext);
		this.inverseRelationshipNodeContext = Objects.requireNonNull(inverseRelationshipNodeContext);
	}

	public void doWithElement(Object relatedElementValue) {

		Object relatedValueToBeSaved = inverseRelationshipNodeContext
			.identifyAndExtractRelationshipValue(relatedElementValue);
		relatedValueToBeSaved = rootNodeContext.getEventSupport().maybeCallBeforeBind(relatedValueToBeSaved);

		Long relatedInternalId = saveRelatedNode(relatedValueToBeSaved,
			inverseRelationshipNodeContext.getAssociationTargetType(),
			inverseRelationshipNodeContext.getInverseNodeDescription());

		// handle creation of relationship depending on properties on relationship or not
		RelationshipStatementHolder statementHolder = inverseRelationshipNodeContext.hasRelationshipWithProperties()
			? createStatementForRelationShipWithProperties(rootNodeContext.getMappingContext(),
			parentEntityContext.getParentPersistentEntity(),
			inverseRelationshipNodeContext,
			relatedInternalId,
			(Map.Entry) relatedValueToBeSaved)
			: createStatementForRelationshipWithoutProperties(parentEntityContext.getParentPersistentEntity(),
			inverseRelationshipNodeContext,
			relatedInternalId,
			relatedValueToBeSaved);

		rootNodeContext.getNeo4jClient().query(renderer.render(statementHolder.getRelationshipCreationQuery()))
			.in(rootNodeContext.getInDatabase())
			.bind(parentEntityContext.getFromId()).to(FROM_ID_PARAMETER_NAME)
			.bindAll(statementHolder.getProperties())
			.run();

		// if an internal id is used this must get set to link this entity in the next iteration
		if (parentEntityContext.getParentPersistentEntity().isUsingInternalIds()) {
			PersistentPropertyAccessor<?> targetPropertyAccessor = inverseRelationshipNodeContext
				.getInverseNodeDescription()
				.getPropertyAccessor(relatedValueToBeSaved);
			targetPropertyAccessor
				.setProperty(parentEntityContext.getParentPersistentEntity().getRequiredIdProperty(),
					relatedInternalId);
		}
		// next recursion level setup
		// TODO: explore if visiting associations with "inverse" set can be avoided
		AssociationParentEntityContext nextLevelParentEntityContext = new AssociationParentEntityContext(
			relatedValueToBeSaved,
			inverseRelationshipNodeContext.getInverseNodeDescription());
		ImperativeAssociationHandler nextLevelAssociationHandler = new ImperativeAssociationHandler(
			rootNodeContext,
			nextLevelParentEntityContext);
		nextLevelParentEntityContext.getParentPersistentEntity().doWithAssociations(nextLevelAssociationHandler);
		//		processNestedAssociations(targetNodeDescription, valueToBeSaved, inDatabase,
		//			processedRelationshipDescriptions);

	}

	private <Y> Long saveRelatedNode(Object entity, Class<Y> entityType, NodeDescription targetNodeDescription) {

		return rootNodeContext.getNeo4jClient()
			.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription)))
			.in(rootNodeContext.getInDatabase())
			.bind((Y) entity).with(rootNodeContext.getMappingContext().getRequiredBinderFunctionFor(entityType))
			.fetchAs(Long.class).one().get();
	}
}
