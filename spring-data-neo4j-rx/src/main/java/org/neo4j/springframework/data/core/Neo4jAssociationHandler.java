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

import java.util.Objects;

import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

/**
 * A graph database like Neo4j requires specific handling of associations.
 * An association is in this case either a property of a node in the graph
 * or a relationship to another node in the graph
 * It provides cohesion and context, common to all associations.
 *
 * @param <P>
 * @author Philipp Tölle
 * @since 1.0
 */
public abstract class Neo4jAssociationHandler<T> implements AssociationHandler<Neo4jPersistentProperty> {

	private final PersistentRootNodeContext rootNodeContext;
	private final AssociationParentEntityContext<T> parentEntityContext;

	protected Neo4jAssociationHandler(
		PersistentRootNodeContext rootNodeContext,
		AssociationParentEntityContext<T> parentEntityContext) {

		this.rootNodeContext = Objects.requireNonNull(rootNodeContext);
		this.parentEntityContext = Objects.requireNonNull(parentEntityContext);
	}

	protected InverseRelationshipNodeContext createInverseNodeContext(
		Association<Neo4jPersistentProperty> association) {

		Neo4jPersistentProperty inverse = association.getInverse();

		Object inverseValue = parentEntityContext.getParentPropertyAccessor().getProperty(inverse);

		RelationshipDescription relationship = parentEntityContext.getParentPersistentEntity()
			.getRelationships().stream()
			.filter(r -> r.getFieldName().equals(inverse.getName()))
			.findFirst().get();

		return InverseRelationshipNodeContext
			.of(inverse, inverseValue, relationship, rootNodeContext.getMappingContext());
	}

	public AssociationParentEntityContext<T> getParentEntityContext() {
		return parentEntityContext;
	}
}
