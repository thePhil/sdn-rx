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
import org.neo4j.driver.StatementRunner;

/**
 * A callback that retrieves a {@link org.neo4j.driver.StatementRunner} to execute arbitrary database calls. The statement runner
 * will participate in ongoing transactions when used in a setup with using the
 * {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}.
 *
 * @param <T> The result type
 * @author Michael J. Simons
 * @since 1.0
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 */
@API(status = API.Status.STABLE, since = "1.0")
@FunctionalInterface
public interface StatementRunnerCallback<T> {

	/**
	 * Gets called by the template and clients of this API can use the supplied runner for as many statements as needed.
	 *
	 * @param statementRunner A statement runner participating in ongoing transactions
	 * @return A possible result (may be empty, but not null)
	 */
	Optional<T> doWithRunner(StatementRunner statementRunner);
}
