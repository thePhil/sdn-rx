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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.data.neo4j.core.Neo4jClientTest.Bike;
import org.springframework.data.neo4j.core.Neo4jClientTest.BikeOwner;
import org.springframework.data.neo4j.core.Neo4jClientTest.BikeOwnerBinder;
import org.springframework.data.neo4j.core.Neo4jClientTest.BikeOwnerReader;

/**
 * @author Michael J. Simons
 */
class ReactiveNeo4jClientTest {

	private final Driver driver;

	private final RxSession session;

	private final RxResult statementResult;

	ReactiveNeo4jClientTest() {

		driver = mock(Driver.class);
		session = mock(RxSession.class);
		statementResult = mock(RxResult.class);

		when(driver.rxSession(any(Consumer.class))).thenReturn(session);
		when(session.run(anyString(), any(Map.class))).thenReturn(statementResult);
		when(statementResult.records()).thenReturn(Flux.empty());
	}

	@Test
	@DisplayName("Creation of queries and binding parameters should feel natural")
	void queryCreationShouldFeelGood() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("bikeName", "M.*");
		parameters.put("location", "Sweden");

		Flux<Map<String, Object>> usedBikes = client
			.newQuery(
				"MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike) - [:USED_ON] -> (t:Trip) " +
					"WHERE t.takenOn > $aDate " +
					"  AND b.name =~ $bikeName " +
					"  AND t.location = $location " +  // TODO Nice place to add coordinates
					"RETURN b"
			)
			.bind("michael").to("name")
			.bindAll(parameters)
			.bind(LocalDate.of(2019, 1, 1)).to("aDate")
			.fetch()
			.all();
	}

	@Test
	void databaseSelectionShouldBePossibleOnlyOnce() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
		Flux<Map<String, Object>> users = client
			.newQuery("MATCH (u:User) WHERE u.name =~ $name")
			.in("bikingDatabase")
			.bind("Someone.*").to("name")
			.fetch()
			.all();
	}

	@Test
	void callbackHandlingShouldFeelGood() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);
		client
			.with("aDatabase")
			.delegateTo(runner -> Mono.empty());
	}

	@Test
	@DisplayName("Mapping should feel good")
	void mappingShouldFeelGood() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		Flux<BikeOwner> bikeOwners = client
			.newQuery(
				"MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike)" +
					"RETURN o, collect(b) as bikes"
			)
			.bind("michael").to("name")
			.fetchAs(BikeOwner.class).mappedBy(new BikeOwnerReader())
			.all();

		BikeOwner michael = new BikeOwner("Michael", Arrays
			.asList(new Bike("Road"), new Bike("MTB")));
		Mono<ResultSummary> resultSummary = client
			.newQuery(
				"MERGE (u:User {name: 'Michael'}) "
					+ "WITH u UNWIND $bikes as bike "
					+ "MERGE (b:Bike {name: bike}) "
					+ "MERGE (u) - [o:OWNS] -> (b) ")
			.bind(michael).with(new BikeOwnerBinder())
			.run();
	}

	@Test
	@DisplayName("Some automatic conversion is ok")
	void someTypesShouldNeedNoMapper() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		Mono<Long> numberOfBikes = client
			.newQuery("MATCH (b:Bike) RETURN count(b)")
			.fetchAs(Long.class)
			.one();
	}

	@Test
	@DisplayName("Queries that return nothing should fit in")
	void queriesWithoutResultShouldFitInAsWell() {

		ReactiveNeo4jClient client = ReactiveNeo4jClient.create(driver);

		Mono<ResultSummary> resultSummary = client
			.newQuery("DETACH DELETE (b) WHERE name = $name")
			.bind("fixie").to("name")
			.run();
	}
}
