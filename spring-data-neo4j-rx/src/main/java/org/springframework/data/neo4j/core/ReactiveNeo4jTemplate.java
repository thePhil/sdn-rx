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

/**
 * Reactive variant of the {@link Neo4jTemplate}. While the record mapping mechanism works as the blocking approach,
 * especially all methods taking in a result set need to deal with reactive ("Rx") variant of the result set.
 *
 * @author Michael J. Simons
 * @since 1.0
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 */
public class ReactiveNeo4jTemplate implements ReactiveNeo4jOperations {
}
