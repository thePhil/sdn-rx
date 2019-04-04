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

import static java.util.stream.Collectors.*;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.StatementRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.data.neo4j.core.transaction.NativeTransactionProvider;

/**
 * Default implementation of {@link Neo4jClient}. Uses the Neo4j Java driver to connect to and interact with the database.
 * TODO Micrometer hooks for statement results...
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class DefaultNeo4jClient implements Neo4jClient {
	private final Driver driver;

	private final NativeTransactionProvider transactionProvider;

	DefaultNeo4jClient(Driver driver, NativeTransactionProvider transactionProvider) {

		this.driver = driver;
		this.transactionProvider = transactionProvider;
	}

	AutoCloseableStatementRunner getStatementRunner() {

		StatementRunner statementRunner = transactionProvider.retrieveTransaction(driver, null)
			.map(StatementRunner.class::cast)
			.orElseGet(() -> driver.session());

		return (AutoCloseableStatementRunner) Proxy.newProxyInstance(StatementRunner.class.getClassLoader(),
			new Class<?>[] { AutoCloseableStatementRunner.class },
			new AutoCloseableStatementRunnerHandler(statementRunner));
	}

	@Override
	public RunnableSpec newQuery(String cypher) {
		return newQuery(() -> cypher);
	}

	@Override
	public RunnableSpec newQuery(Supplier<String> cypherSupplier) {
		return new DefaultRunnableSpec(cypherSupplier);
	}

	@RequiredArgsConstructor
	class DefaultRunnableSpec implements RunnableSpec {

		private final Supplier<String> cypherSupplier;

		// TODO Make the evaluation of binders lazy
		private final Map<String, Object> parameters = new HashMap<>();

		@RequiredArgsConstructor
		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, RunnableSpec> {

			private final T value;

			@Override
			public RunnableSpec to(String name) {
				return bindAll(Collections.singletonMap(name, value));
			}

			@Override
			public RunnableSpec with(Function<T, Map<String, Object>> binder) {
				return bindAll(binder.apply(value));
			}
		}

		@Override
		public OngoingBindSpec<?, RunnableSpec> bind(Object value) {
			return new DefaultOngoingBindSpec(value);
		}

		@Override
		public RunnableSpec bindAll(Map<String, Object> newParameters) {
			newParameters.forEach((k, v) -> {
				if (parameters.containsKey(k)) {
					Object previousValue = parameters.get(k);
					throw new IllegalArgumentException(String.format(
						"Duplicate parameter name: '%s' already in the list of named parameters with value '%s'. New value would be '%s'",
						k,
						previousValue == null ? "null" : previousValue.toString(),
						v == null ? "null" : v.toString()
					));
				}
				parameters.put(k, v);
			});

			return this;
		}

		@Override
		public <R> MappingSpec<R> fetchAs(Class<R> targetClass) {
			return new DefaultRecordFetchSpec(cypherSupplier, parameters);
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {
			return new DefaultRecordFetchSpec<>(
				cypherSupplier,
				parameters, Record::asMap);
		}

		@Override
		public ResultSummary run() {
			try (AutoCloseableStatementRunner statementRunner = getStatementRunner()) {
				StatementResult result = statementRunner.run(cypherSupplier.get(), parameters);
				return result.consume();
			}
		}
	}

	@AllArgsConstructor
	@RequiredArgsConstructor
	class DefaultRecordFetchSpec<T> implements RecordFetchSpec<T>, MappingSpec<T> {

		private final Supplier<String> cypherSupplier;

		private final Map<String, Object> parameters;

		private Function<Record, T> mappingFunction;

		@Override
		public RecordFetchSpec<T> mappedBy(@SuppressWarnings("HiddenField") Function<Record, T> mappingFunction) {
			this.mappingFunction = mappingFunction;
			return this;
		}

		@Override
		public Optional<T> one() {
			try (AutoCloseableStatementRunner statementRunner = getStatementRunner()) {
				StatementResult result = statementRunner.run(cypherSupplier.get(), parameters);
				return result.hasNext() ? Optional.of(mappingFunction.apply(result.single())) : Optional.empty();
			}
		}

		@Override
		public Optional<T> first() {
			try (AutoCloseableStatementRunner statementRunner = getStatementRunner()) {
				StatementResult result = statementRunner.run(cypherSupplier.get(), parameters);
				return result.stream().map(mappingFunction).findFirst();
			}
		}

		@Override
		public Collection<T> all() {
			try (AutoCloseableStatementRunner statementRunner = getStatementRunner()) {
				StatementResult result = statementRunner.run(cypherSupplier.get(), parameters);
				return result.stream().map(mappingFunction).collect(toList());
			}
		}
	}

	/**
	 * Makes a statement runner autoclosable and aware wether it's session or a transaction
	 */
	interface AutoCloseableStatementRunner extends StatementRunner, AutoCloseable {

		@Override void close();
	}

	private static class AutoCloseableStatementRunnerHandler implements InvocationHandler {

		private final Map<Method, MethodHandle> cachedHandles = new ConcurrentHashMap<>();
		private final StatementRunner target;

		AutoCloseableStatementRunnerHandler(StatementRunner target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			if ("close".equals(method.getName())) {
				if (this.target instanceof Session) {
					((Session) this.target).close();
				}
				return null;
			} else {
				return cachedHandles.computeIfAbsent(method, this::findHandleFor)
					.invokeWithArguments(args);
			}
		}

		MethodHandle findHandleFor(Method method) {
			try {
				return MethodHandles.publicLookup().unreflect(method).bindTo(target);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
