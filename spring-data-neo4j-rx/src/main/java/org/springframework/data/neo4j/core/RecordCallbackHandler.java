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

import org.apiguardian.api.API;
import org.neo4j.driver.Record;

/**
 * This interface can be passed to the {@link Neo4jTemplate} to process records of a {@link org.neo4j.driver.StatementResult}
 * or {@link org.neo4j.driver.reactive.RxResult} in a stateful manner. Therefore instances of this interface should not be
 * reused (being passed multiple times to a {@link Neo4jTemplate}.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Bis zum bitteren Ende
 */
@API(status = API.Status.STABLE, since = "1.0")
@FunctionalInterface
public interface RecordCallbackHandler {

	/**
	 * Called by the {@link Neo4jTemplate} with each record inside the result set in the order of which records have been
	 * returned by the underlying query.
	 *
	 * @param record The record to be processed.
	 */
	void processRecord(Record record);
}
