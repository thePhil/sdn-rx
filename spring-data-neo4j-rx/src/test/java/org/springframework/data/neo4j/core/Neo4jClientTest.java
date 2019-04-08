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

import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;

/**
 * @author Michael J. Simons
 */
class Neo4jClientTest {

	private final Driver driver;

	private final Session session;

	private final StatementResult statementResult;

	Neo4jClientTest() {

		driver = mock(Driver.class);
		session = mock(Session.class);
		statementResult = mock(StatementResult.class);

		when(driver.session(any(Consumer.class))).thenReturn(session);
		when(session.run(anyString(), any(Map.class))).thenReturn(statementResult);
		when(statementResult.stream()).thenReturn(Collections.<Record>emptyList().stream());
	}

	@Test
	@DisplayName("Creation of queries and binding parameters should feel natural")
	void queryCreationShouldFeelGood() {

		Neo4jClient client = Neo4jClient.create(driver);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("bikeName", "M.*");
		parameters.put("location", "Sweden");

		Collection<Map<String, Object>> usedBikes = client
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

		Neo4jClient client = Neo4jClient.create(driver);
		Collection<Map<String, Object>> users = client
			.newQuery("MATCH (u:User) WHERE u.name =~ $name")
			.in("bikingDatabase")
			.bind("Someone.*").to("name")
			.fetch()
			.all();
	}

	@Test
	void callbackHandlingShouldFeelGood() {

		Neo4jClient client = Neo4jClient.create(driver);
		client
			.with("aDatabase")
			.delegateTo(runner -> Optional.empty());
	}

	@Test
	@DisplayName("Mapping should feel good")
	void mappingShouldFeelGood() {

		Neo4jClient client = Neo4jClient.create(driver);

		Collection<BikeOwner> bikeOwners = client
			.newQuery(
				"MATCH (o:User {name: $name}) - [:OWNS] -> (b:Bike)" +
					"RETURN o, collect(b) as bikes"
			)
			.bind("michael").to("name")
			.fetchAs(BikeOwner.class).mappedBy(new BikeOwnerReader())
			.all();

		BikeOwner michael = new BikeOwner("Michael", Arrays.asList(new Bike("Road"), new Bike("MTB")));
		client
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

		Neo4jClient client = Neo4jClient.create(driver);

		Optional<Long> numberOfBikes = client
			.newQuery("MATCH (b:Bike) RETURN count(b)")
			.fetchAs(Long.class)
			.one();
	}

	@Test
	@DisplayName("Queries that return nothing should fit in")
	void queriesWithoutResultShouldFitInAsWell() {

		Neo4jClient client = Neo4jClient.create(driver);

		client
			.newQuery("DETACH DELETE (b) WHERE name = $name")
			.bind("fixie").to("name")
			.run();
	}

	static class BikeOwner {

		private final String name;

		private final List<Bike> bikes;

		BikeOwner(String name, List<Bike> bikes) {
			this.name = name;
			this.bikes = new ArrayList<>(bikes);
		}

		public String getName() {
			return name;
		}

		public List<Bike> getBikes() {
			return Collections.unmodifiableList(bikes);
		}
	}

	static class Bike {

		private final String name;

		Bike(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	static class BikeOwnerReader implements Function<Record, BikeOwner> {

		@Override
		public BikeOwner apply(Record record) {
			return null;
		}
	}

	static class BikeOwnerBinder implements Function<BikeOwner, Map<String, Object>> {

		@Override
		public Map<String, Object> apply(BikeOwner bikeOwner) {

			return Collections.emptyMap();
		}
	}
}
