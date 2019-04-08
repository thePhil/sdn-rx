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

import static org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils.*;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxStatementRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.reactivestreams.Publisher;
import org.springframework.data.neo4j.core.Neo4jClient.MappingSpec;
import org.springframework.data.neo4j.core.Neo4jClient.OngoingBindSpec;
import org.springframework.data.neo4j.core.Neo4jClient.RecordFetchSpec;

/**
 * Reactive variant of the {@link Neo4jClient}.
 *
 * TODO Reactive transaction management is pretty obvious still open
 *
 * @author Michael J. Simons
 * @since 1.0
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 */
class DefaultReactiveNeo4jClient implements ReactiveNeo4jClient {

	private final Driver driver;

	DefaultReactiveNeo4jClient(Driver driver) {

		this.driver = driver;
	}

	// Internal helper methods for managing transactional state

	Mono<RxStatementRunner> getStatementRunner(final String targetDatabase) {

		return retrieveReactiveTransaction(driver, targetDatabase)
			.map(RxStatementRunner.class::cast)
			// Open the transaction inside a supplier to avoid eager initialization
			.switchIfEmpty(Mono.fromSupplier(() -> driver.rxSession(defaultSessionParameters(targetDatabase))));
	}

	static Publisher<Void> asyncComplete(RxStatementRunner runner) {

		if (runner instanceof RxSession) {
			return ((RxSession) runner).close();
		}

		return Mono.empty();
	}

	static Publisher<Void> asyncError(RxStatementRunner runner) {

		if (runner instanceof RxSession) {
			return ((RxSession) runner).close();
		}

		return Mono.empty();
	}

	// Below are all the implementations (methods and classes) as defined by the contracts of ReactiveNeo4jClient

	@Override
	public ReactiveRunnableSpec newQuery(String cypher) {
		return newQuery(() -> cypher);
	}

	@Override
	public ReactiveRunnableSpec newQuery(Supplier<String> cypherSupplier) {
		return new DefaultReactiveRunnableSpec(cypherSupplier);
	}

	@Override
	public OngoingReactiveDelegation with(String targetDatabase) {

		return callback -> Mono.usingWhen(
			getStatementRunner(targetDatabase),
			rxStatementRunner -> callback.doWithRunner(rxStatementRunner),
			DefaultReactiveNeo4jClient::asyncComplete,
			DefaultReactiveNeo4jClient::asyncError);

	}

	@RequiredArgsConstructor
	class DefaultReactiveRunnableSpec implements ReactiveRunnableSpec {

		private final Supplier<String> cypherSupplier;

		private String targetDatabase;

		// TODO Make the evaluation of binders lazy
		private final Map<String, Object> parameters = new HashMap<>();

		@Override
		public DefaultReactiveRunnableSpec in(@SuppressWarnings("HiddenField") String targetDatabase) {
			this.targetDatabase = targetDatabase;
			return this;
		}

		@RequiredArgsConstructor
		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, ReactiveRunnableSpecTightToDatabase> {

			private final T value;

			@Override
			public ReactiveRunnableSpecTightToDatabase to(String name) {
				return bindAll(Collections.singletonMap(name, value));
			}

			@Override
			public ReactiveRunnableSpecTightToDatabase with(Function<T, Map<String, Object>> binder) {
				return bindAll(binder.apply(value));
			}
		}

		@Override
		public OngoingBindSpec<?, ReactiveRunnableSpecTightToDatabase> bind(Object value) {
			return new DefaultOngoingBindSpec(value);
		}

		@Override
		public ReactiveRunnableSpecTightToDatabase bindAll(Map<String, Object> newParameters) {
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
		public <R> MappingSpec<Mono<R>, Flux<R>, R> fetchAs(Class<R> targetClass) {

			return new DefaultReactiveRecordFetchSpec<>(this.targetDatabase, this.cypherSupplier, this.parameters);
		}

		@Override
		public RecordFetchSpec<Mono<Map<String, Object>>, Flux<Map<String, Object>>, Map<String, Object>> fetch() {

			return new DefaultReactiveRecordFetchSpec<>(targetDatabase, cypherSupplier, parameters, Record::asMap);
		}

		@Override
		public Mono<ResultSummary> run() {

			return Mono
				.usingWhen(
					getStatementRunner(targetDatabase),
					statementRunner -> Mono.from(statementRunner.run(cypherSupplier.get(), parameters).summary()),
					DefaultReactiveNeo4jClient::asyncComplete,
					DefaultReactiveNeo4jClient::asyncError
				);
		}
	}

	@AllArgsConstructor
	@RequiredArgsConstructor
	class DefaultReactiveRecordFetchSpec<T>
		implements RecordFetchSpec<Mono<T>, Flux<T>, T>, MappingSpec<Mono<T>, Flux<T>, T> {

		private final String targetDatabase;

		private final Supplier<String> cypherSupplier;

		private final Map<String, Object> parameters;

		private Function<Record, T> mappingFunction;

		@Override
		public RecordFetchSpec<Mono<T>, Flux<T>, T> mappedBy(Function<Record, T> mappingFunction) {

			this.mappingFunction = mappingFunction;
			return this;
		}

		@Override
		public Mono<T> one() {

			return Mono.usingWhen(
				getStatementRunner(targetDatabase),
				runner -> Mono.from(runner.run(cypherSupplier.get(), parameters).records()).map(mappingFunction)
					.single(),
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError
			);
		}

		@Override
		public Mono<T> first() {

			return Mono.usingWhen(
				getStatementRunner(targetDatabase),
				runner -> Flux.from(runner.run(cypherSupplier.get(), parameters).records()).map(mappingFunction).next(),
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError
			);
		}

		@Override
		public Flux<T> all() {

			return Flux.usingWhen(
				getStatementRunner(targetDatabase),
				runner -> Flux.from(runner.run(cypherSupplier.get(), parameters).records()).map(mappingFunction),
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError
			);
		}
	}
}
