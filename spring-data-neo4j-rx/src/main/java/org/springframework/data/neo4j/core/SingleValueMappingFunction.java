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

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

/**
 * Used to automagically map single valued records to a sensible Java type based on {@link Value#asObject()}.
 *
 * @param <T>
 * @author Michael J. Simons
 */
@RequiredArgsConstructor
final class SingleValueMappingFunction<T> implements Function<Record, T> {

	private final Class<T> targetClass;

	@Override
	public T apply(Record record) {

		if (record.size() != 1) {
			throw new IllegalArgumentException(
				"Records with more than one value cannot be converted without a mapper.");
		}

		Object o = record.get(0).asObject();

		if (o == null) {
			return null;
		}

		if (!targetClass.isAssignableFrom(o.getClass())) {
			throw new IllegalArgumentException(
				String.format("%s is not assignable from %s", targetClass.getName(), o.getClass().getName()));
		}

		return targetClass.cast(o);
	}
}
