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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.util.Assert;

/**
 * Working on nested relationships happens in a certain algorithmic context.
 * This context enables a tight cohesion between the algorithmic steps and the data, these steps are performed on.
 * In our the interaction happens between the data that describes the relationship and the specific steps of
 * the algorithm.
 *
 * @author Philipp Tölle
 * @since 1.0
 */
final class InverseRelationshipNodeContext {

	//TODO: make nonnull
	private final Neo4jPersistentProperty inverse;
	private final Object value;
	private final RelationshipDescription relationship;
	private final Class<?> associationTargetType;
	private final Neo4jPersistentEntity<?> inverseNodeDescription;

	private final boolean inverseValueIsEmpty;

	private InverseRelationshipNodeContext(Neo4jPersistentProperty inverse,
		Object value,
		RelationshipDescription relationship,
		Neo4jMappingContext mappingContext
	) {
		this.inverse = inverse;
		this.value = value;
		this.relationship = relationship;

		// if we have a relationship with properties, the targetNodeType is the map key
		this.associationTargetType = relationship.hasRelationshipProperties()
			? inverse.getComponentType()
			: inverse.getAssociationTargetType();
		this.inverseValueIsEmpty = value == null;

		this.inverseNodeDescription = mappingContext.getPersistentEntity(this.associationTargetType);
	}

	Neo4jPersistentProperty getInverse() {
		return inverse;
	}

	Object getValue() {
		return value;
	}

	RelationshipDescription getRelationship() {
		return relationship;
	}

	Class<?> getAssociationTargetType() {
		return associationTargetType;
	}

	public boolean inverseValueIsEmpty() {
		return inverseValueIsEmpty;
	}

	boolean hasRelationshipWithProperties() {
		return this.relationship.hasRelationshipProperties();
	}

	Object identifyAndExtractRelationshipValue(Object relatedValue) {
		Object valueToBeSaved = relatedValue;
		if (relatedValue instanceof Map.Entry) {
			Map.Entry relatedValueMapEntry = (Map.Entry) relatedValue;

			if (this.getInverse().isDynamicAssociation()) {
				valueToBeSaved = relatedValueMapEntry.getValue();
			} else if (this.hasRelationshipWithProperties()) {
				valueToBeSaved = relatedValueMapEntry.getKey();
			}
		}

		return valueToBeSaved;
	}

	public Neo4jPersistentEntity<?> getInverseNodeDescription() {
		return inverseNodeDescription;
	}

	/**
	 * The value for a relationship can be a scalar object (1:1), a collection (1:n), a map (1:n, but with dynamic
	 * relationship types) or a map (1:n) with properties for each relationship.
	 * This method unifies the type into something iterable, depending on the given inverse type.
	 *
	 * @param rawValue The raw value to unify
	 * @return A unified collection (Either a collection of Map.Entry for dynamic and relationships with properties
	 * or a list of related values)
	 */
	private Collection<?> unifyInverseValue() {
		Collection<?> unifiedValue;
		if (inverse.isDynamicAssociation()) {
			unifiedValue = ((Map<String, Object>) value).entrySet();
		} else if (inverse.isRelationshipWithProperties()) {
			unifiedValue = ((Map<Object, Object>) value).entrySet();
		} else if (inverse.isCollectionLike()) {
			unifiedValue = (Collection<Object>) value;
		} else {
			unifiedValue = Collections.singleton(value);
		}
		return unifiedValue;
	}

	protected void doWithElements(AssociationElementHandler<?> elementHandler) {

		Assert.notNull(elementHandler, "Handler must not be null!");
		for (Object relatedElementValue : unifyInverseValue()) {
			elementHandler.doWithElement(relatedElementValue);
		}
	}

	static InverseRelationshipNodeContext of(Neo4jPersistentProperty inverse, Object inverseValue,
		RelationshipDescription relationshipDescription,
		Neo4jMappingContext mappingContext) {
		return new InverseRelationshipNodeContext(inverse, inverseValue, relationshipDescription, mappingContext);
	}
}
