/*
 * Copyright 2015-2022 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.webmvc.support.ExceptionMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

/**
 * Unit tests for {@link RepositoryRestExceptionHandler}.
 *
 * @author Oliver Gierke
 */
class RepositoryRestExceptionHandlerUnitTests {

	static final RepositoryRestExceptionHandler HANDLER = new RepositoryRestExceptionHandler(new StaticMessageSource());

	static Logger logger;
	static Level logLevel;

	@BeforeAll
	public static void silenceLog() {

		logger = (Logger) LoggerFactory.getLogger(RepositoryRestExceptionHandler.class);
		logLevel = logger.getLevel();
		logger.setLevel(Level.OFF);
	}

	@AfterAll
	public static void enableLogging() {
		logger.setLevel(logLevel);
	}

	@Test // DATAREST-427
	void handlesHttpMessageNotReadableException() {

		ResponseEntity<ExceptionMessage> result = HANDLER
				.handleNotReadable(new HttpMessageNotReadableException("Message", new MockHttpInputMessage(new byte[0])));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test // DATAREST-507
	void handlesConflictCorrectly() {

		ResponseEntity<ExceptionMessage> result = HANDLER.handleConflict(new DataIntegrityViolationException("Message"));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	static java.util.stream.Stream<Exception> conflictExceptions() {
		return java.util.stream.Stream.of(
				new DataIntegrityViolationException("could not execute statement",
						new RuntimeException("Some message.")),
				new OptimisticLockingFailureException("Some message."));
	}

	@ParameterizedTest // GH-2571
	@MethodSource("conflictExceptions")
	void conflictResponseHasExpectedStatusCode(Exception exception) {

		ResponseEntity<ExceptionMessage> result = HANDLER.handleConflict(exception);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getMessage()).doesNotContain("Some message.");
	}

	static Stream<Exception> miscExceptions() {
		return Stream.of(
				new IllegalArgumentException("Some message."),
				new ClassCastException("Some message."),
				new NullPointerException("Some message."),
				new ConversionFailedException(TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Long.class),
						"some-value", new RuntimeException("Some message.")));
	}

	@ParameterizedTest // GH-2571
	@MethodSource("miscExceptions")
	void miscFailureResponseHasExpectedStatusCode(Exception exception) {

		ResponseEntity<ExceptionMessage> result = HANDLER.handleMiscFailures(exception);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getMessage()).doesNotContain("Some message.");
	}
}
