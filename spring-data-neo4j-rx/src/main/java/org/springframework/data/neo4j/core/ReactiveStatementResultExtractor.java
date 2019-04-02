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

import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.driver.reactive.RxResult;

/**
 * This is the reactive version of the {@link StatementResultExtractor} and is an interface used by {@link Neo4jTemplate}
 * to extract one thing from a {@link RxResult}.
 * <p>
 * This interface is mainly used within the framework itself but may prove to be useful when a result can only be
 * assembled by multiple records from the same statement result. Most of the time, the usage of a {@link RecordMapper}
 * should be simpler.
 * <p>
 * Instances of this interface should usually be stateless and thus reusable.
 *
 * @param <T> The result type
 * @author Michael J. Simons
 * @since 1.0
 * @soundtrack Die Toten Hosen - Bis zum bitteren Ende
 */
@API(status = API.Status.STABLE, since = "1.0")
@FunctionalInterface
public interface ReactiveStatementResultExtractor<T> {

	/**
	 * Implementations must implement this method to map a complete statement result into an object.
	 *
	 * @param statementResult The complete statement from which to extract the result
	 * @return The result of the extraction (may be empty, but not null)
	 */
	Optional<T> extractData(RxResult statementResult);
}
