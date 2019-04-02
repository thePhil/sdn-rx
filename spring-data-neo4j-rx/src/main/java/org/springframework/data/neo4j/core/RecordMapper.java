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
import org.neo4j.driver.Record;

/**
 * An interface used by {@link Neo4jTemplate} for mapping records of a {@link org.neo4j.driver.StatementResult} or {@link org.neo4j.driver.reactive.RxResult}.
 * Implementations of this interface perform the actual work of mapping each record to a result object, but don't need
 * to worry about exception handling. Checked exceptions will be caught and handled by the calling Neo4jTemplate.
 * <p>
 * Instances of this interface are typically used for query methods and are usually stateless and thus reusable, which
 * make them an ideal choice for implementing row-mapping logic in a single place.
 *
 * @param <T> The result type
 * @author Michael J. Simons
 * @since 1.0
 * @soundtrack Die Toten Hosen - Bis zum bitteren Ende
 */
@API(status = API.Status.STABLE, since = "1.0")
@FunctionalInterface
public interface RecordMapper<T> {

	/**
	 * Implementations must implement this method to map each record of data in a result set.
	 *
	 * @param record       The record to be mapped
	 * @param recordNumber The number of the current record
	 * @return The result object for the current record (may be empty, but not null)
	 */
	Optional<T> mapRecord(Record record, int recordNumber);
}
