/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.apiguardian.api.API;

/**
 * Representing a named parameter for a Cypher query.
 *
 * @param <T> Type of the parameters value
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 1.0
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@API(status = API.Status.STABLE, since = "1.0")
public class NamedParameter<T> {

	@NonNull
	private final String name;

	private final T value;

	/**
	 * @return Name of this parameter
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Value of this parameter, may be empty but never null
	 */
	public Optional<T> getValue() {
		return Optional.ofNullable(value);
	}
}
