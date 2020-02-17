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

import static org.neo4j.springframework.data.core.schema.CypherGenerator.*;

import org.apache.commons.logging.LogFactory;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.mapping.Association;

/**
 * This imperative variation of the {@link Neo4jAssociationHandler} contains the details of persisting associations of
 * a node into the database.
 * It provides cohesion and context, common to all associations.
 *
 * @author Philipp Tölle
 */
public class ImperativeAssociationHandler<T> extends Neo4jAssociationHandler<T> {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(ImperativeAssociationHandler.class));

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final PersistentRootNodeContext rootNodeContext;

	public ImperativeAssociationHandler(PersistentRootNodeContext rootNodeContext,
		AssociationParentEntityContext<T> parentEntityContext) {
		super(rootNodeContext, parentEntityContext);
		this.rootNodeContext = rootNodeContext;
	}

	@Override
	public void doWithAssociation(Association<Neo4jPersistentProperty> association) {

		InverseRelationshipNodeContext inverseContext = createInverseNodeContext(association);
		RelationshipDescription relationshipDescription = inverseContext.getRelationship();

		// break recursive procession and deletion of previously created relationships
		RelationshipDescription relationshipObverse = relationshipDescription.getRelationshipObverse();
		if (rootNodeContext.hasProcessed(relationshipObverse)) {
			return;
		}

		// remove all relationships before creating all new if the entity is not new
		// this avoids the usage of cache but might have significant impact on overall performance
		// TODO: implement this step with the listener pattern or with a different strategy
		if (!this.getParentEntityContext().isObverseNew()) {
			Statement relationshipRemoveQuery = rootNodeContext.getCypherGenerator().createRelationshipRemoveQuery(
				getParentEntityContext().getParentPersistentEntity(),
				relationshipDescription,
				inverseContext.getInverseNodeDescription().getPrimaryLabel());

			rootNodeContext.getNeo4jClient().query(renderer.render(relationshipRemoveQuery))
				.in(rootNodeContext.getInDatabase())
				.bind(this.getParentEntityContext().getFromId()).to(FROM_ID_PARAMETER_NAME).run();
		}

		// nothing to do because there is nothing to map
		if (inverseContext.inverseValueIsEmpty()) {
			return;
		}

		rootNodeContext.recordProcessedRelationship(relationshipDescription);
		AssociationElementHandler<?> elementHandler = new AssociationElementHandler<>(rootNodeContext,
			getParentEntityContext(), inverseContext);

		// persist each element of the association (can be one or many)
		inverseContext.doWithElements(elementHandler);
	}
}
