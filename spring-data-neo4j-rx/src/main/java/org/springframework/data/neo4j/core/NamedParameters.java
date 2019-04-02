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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apiguardian.api.API;

/**
 * Utility class for dealing with named parameters.
 *
 * @author Michael J. Simons
 * @soundtrack Roque Traders - Voodoo Child
 */
@API(status = API.Status.INTERNAL, since = "1.0")
final class NamedParameters {

	/**
	 * Utility method to convert a list of named parameters into a map that the Neo4j can deal with.
	 *
	 * @param parameters
	 * @return
	 */
	static Map<String, Object> asParameterMap(NamedParameter<?>... parameters) {

		// Cannot use stream api here as it doesn't allow null values in the map.
		Map<String, Object> parameterMap = new HashMap<>(parameters.length);
		for (NamedParameter namedParameter : parameters) {

			if (parameterMap.containsKey(namedParameter.getName())) {

				Object previousValue = parameterMap.get(namedParameter.getName());

				throw new IllegalArgumentException(String.format(
					"Duplicate parameter name: '%s' already in the list of named parameters with value '%s'. New value would be '%s'",
					namedParameter.getName(),
					previousValue == null ? "null" : previousValue.toString(),
					namedParameter.getValue().map(Object::toString).orElse("null")
				));
			}

			parameterMap.put(namedParameter.getName(), namedParameter.getValue().orElse(null));
		}

		return Collections.unmodifiableMap(parameterMap);
	}

	private NamedParameters() {
	}
}
