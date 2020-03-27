/*
 * Copyright 2015-2018 original author or authors.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
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
public class RepositoryRestExceptionHandlerUnitTests {

	static final RepositoryRestExceptionHandler HANDLER = new RepositoryRestExceptionHandler(new StaticMessageSource());

	static Logger logger;
	static Level logLevel;

	@BeforeClass
	public static void silenceLog() {

		logger = (Logger) LoggerFactory.getLogger(RepositoryRestExceptionHandler.class);
		logLevel = logger.getLevel();
		logger.setLevel(Level.OFF);
	}

	@AfterClass
	public static void enableLogging() {
		logger.setLevel(logLevel);
	}

	@Test // DATAREST-427
	public void handlesHttpMessageNotReadableException() {

		ResponseEntity<ExceptionMessage> result = HANDLER
				.handleNotReadable(new HttpMessageNotReadableException("Message!", new MockHttpInputMessage(new byte[0])));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test // DATAREST-507
	public void handlesConflictCorrectly() {

		ResponseEntity<ExceptionMessage> result = HANDLER.handleConflict(new DataIntegrityViolationException("Message!"));

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test // DATAREST-706
	public void forwardsExceptionForMiscellaneousFailure() {

		String message = "My Message!";

		ResponseEntity<ExceptionMessage> result = HANDLER.handleMiscFailures(new Exception(message));

		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getMessage()).isEqualTo(message);
	}
}
