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

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class NamedParametersTest {

	@Test
	void shouldConvertCorrectListOfParametersIntoMap() {

		Map<String, Object> parameters = NamedParameters.asParameterMap(
			NamedParameter.builder().name("a").value(1).build(),
			NamedParameter.builder().name("b").value("Something").build(),
			NamedParameter.builder().name("c").value(null).build()
		);

		assertThat(parameters)
			.containsEntry("a", 1)
			.containsEntry("b", "Something")
			.containsEntry("c", null);
	}

	@Test
	void shouldNotAllowDuplicateParameters() {

		assertThatIllegalArgumentException().isThrownBy(() ->
			NamedParameters.asParameterMap(
				NamedParameter.builder().name("a").value(1).build(),
				NamedParameter.builder().name("b").value(1).build(),
				NamedParameter.builder().name("a").value(2).build()
			)
		).withMessage(
			"Duplicate parameter name: 'a' already in the list of named parameters with value '1'. New value would be '2'");

		assertThatIllegalArgumentException().isThrownBy(() ->
			NamedParameters.asParameterMap(
				NamedParameter.builder().name("a").value(null).build(),
				NamedParameter.builder().name("a").value(2).build()
			)
		).withMessage(
			"Duplicate parameter name: 'a' already in the list of named parameters with value 'null'. New value would be '2'");

		assertThatIllegalArgumentException().isThrownBy(() ->
			NamedParameters.asParameterMap(
				NamedParameter.builder().name("a").value(1).build(),
				NamedParameter.builder().name("a").value(null).build()
			)
		).withMessage(
			"Duplicate parameter name: 'a' already in the list of named parameters with value '1'. New value would be 'null'");
	}

	@Test
	void shouldDealWithEmptyParameterList() {

		assertThat(NamedParameters.asParameterMap()).isEmpty();
	}
}
