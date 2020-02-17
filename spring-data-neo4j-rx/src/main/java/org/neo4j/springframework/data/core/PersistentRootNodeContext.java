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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.schema.CypherGenerator;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.lang.Nullable;

/**
 * This class bundles the context that has the scope of the entire lifetime after {@link org.neo4j.springframework.data.repository.Neo4jRepository#save}
 * has been called.
 *
 * This way we can maintain a tight cohesion for this concern and also simplify passing around of the context
 *
 * @author Philipp Tölle
 */
public class PersistentRootNodeContext {
	private final Set<RelationshipDescription> processedRelationshipDescriptions;
	private final @Nullable String inDatabase;

	private final CypherGenerator cypherGenerator;
	private final Neo4jMappingContext mappingContext;
	private final Neo4jClient neo4jClient;
	private final Neo4jTemplate.Neo4jEvents eventSupport;

	public PersistentRootNodeContext(
		@Nullable String inDatabase,
		CypherGenerator cypherGenerator, Neo4jMappingContext mappingContext,
		Neo4jClient neo4jClient, Neo4jTemplate.Neo4jEvents eventSupport) {
		this.processedRelationshipDescriptions = new HashSet<>();
		this.inDatabase = inDatabase;
		this.cypherGenerator = Objects.requireNonNull(cypherGenerator);
		this.mappingContext = Objects.requireNonNull(mappingContext);
		this.neo4jClient = Objects.requireNonNull(neo4jClient);
		this.eventSupport = Objects.requireNonNull(eventSupport);
	}

	protected boolean hasProcessed(RelationshipDescription relationshipDescription) {
		if (relationshipDescription != null) {
			return processedRelationshipDescriptions.contains(relationshipDescription);
		}
		return false;
	}

	@Nullable public String getInDatabase() {
		return inDatabase;
	}

	protected void recordProcessedRelationship(RelationshipDescription relationshipDescription) {
		processedRelationshipDescriptions.add(relationshipDescription);
	}

	public CypherGenerator getCypherGenerator() {
		return cypherGenerator;
	}

	public Neo4jClient getNeo4jClient() {
		return neo4jClient;
	}

	public Neo4jMappingContext getMappingContext() {
		return mappingContext;
	}

	public Neo4jTemplate.Neo4jEvents getEventSupport() {
		return eventSupport;
	}
}
