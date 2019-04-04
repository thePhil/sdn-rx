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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.data.neo4j.core.transaction.ManagedTransactionProvider;

/**
 * Definition of a modern Neo4j client.
 *
 * TODO Create examples how to use the callbacks etc. with Springs TransactionTemplate to deal with rollbacks etc.
 * TODO database selection
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Neo4jClient {

	static Neo4jClient create(Driver driver) {

		return new DefaultNeo4jClient(driver, new ManagedTransactionProvider());
	}

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether it's a match, merge, create or
	 * removal of things.
	 *
	 * @param cypher The cypher code that shall be executed
	 * @return A new CypherSpec
	 */
	RunnableSpec newQuery(String cypher);

	/**
	 * Entrypoint for creating a new Cypher query based on a supplier. Doesn't matter at this point whether it's a match,
	 * merge, create or removal of things. The supplier can be an arbitrary Supplier that may provide a DSL for generating
	 * the Cypher statement.
	 *
	 * @param cypherSupplier A supplier of arbitrary Cypher code
	 * @return
	 */
	RunnableSpec newQuery(Supplier<String> cypherSupplier);

	/**
	 * Contract for a runnable query that can be either run returning it's result, run without results or be parameterized.
	 */
	interface RunnableSpec extends BindSpec<RunnableSpec> {

		/**
		 * Create a mapping for each record return to a specific type.
		 *
		 * @param targetClass The class each record should be mapped to
		 * @param <R>         The type of the class
		 * @return A mapping spec that allows specifying a mapping function
		 */
		<R> MappingSpec<R> fetchAs(Class<R> targetClass);

		/**
		 * Fetch all records mapped into generic maps
		 *
		 * @return A fetch specification that maps into generic maps
		 */
		RecordFetchSpec<Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results
		 *
		 * @return
		 */
		ResultSummary run();
	}

	/**
	 * Contract for binding parameters to a query.
	 *
	 * @param <S> This {{@link BindSpec specs}} own type
	 */
	interface BindSpec<S extends BindSpec<S>> {

		/**
		 * @param value The value to bind to a query
		 * @return An ongoing bind spec for specifying the name that {@code value} should be bound to or a binder function
		 */
		<T> OngoingBindSpec<T, S> bind(T value);

		S bindAll(Map<String, Object> parameters);
	}

	/**
	 * Ongoing bind specification.
	 *
	 * @param <S> This {{@link OngoingBindSpec specs}} own type
	 */
	interface OngoingBindSpec<T, S extends BindSpec<S>> {

		/**
		 * Bind one convertible object to the given name.
		 *
		 * @param name The named parameter to bind the value to
		 * @return
		 */
		S to(String name);

		/**
		 * Use a binder function for the previously defined value.
		 *
		 * @param binder The binder function to create a map of parameters from the given value
		 * @return
		 */
		S with(Function<T, Map<String, Object>> binder);
	}

	interface MappingSpec<R> extends RecordFetchSpec<R> {

		RecordFetchSpec<R> mappedBy(Function<Record, R> mappingFunction);
	}

	interface RecordFetchSpec<T> {

		Optional<T> one();

		Optional<T> first();

		Collection<T> all();
	}
}
