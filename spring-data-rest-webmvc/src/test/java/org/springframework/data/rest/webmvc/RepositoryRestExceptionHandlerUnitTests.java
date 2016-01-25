/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Unit tests for {@link RepositoryRestExceptionHandler}.
 * 
 * @author Oliver Gierke
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
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

	/**
	 * @see DATAREST-427
	 */
	@Test
	public void handlesHttpMessageNotReadableException() {

		ResponseEntity<ExceptionMessage> result = HANDLER
				.handleNotReadable(new HttpMessageNotReadableException("Message!"));

		assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}

	@Test
	public void handlesHttpMessageResourceNotFoundException() {

		ResponseEntity<?> result = HANDLER
				.handleNotFound(new ResourceNotFoundException());

		assertThat(result.getStatusCode(), is(HttpStatus.NOT_FOUND));
	}
	
	/**
	 * @see DATAREST-755
	 */
	@Test
	public void handlesHttpMessageResourceForbiddenException() {

		ResponseEntity<?> result = HANDLER
				.handleForbidden(new ResourceForbiddenException());

		assertThat(result.getStatusCode(), is(HttpStatus.FORBIDDEN));
	}
	
	/**
	 * @see DATAREST-507
	 */
	@Test
	public void handlesConflictCorrectly() {

		ResponseEntity<ExceptionMessage> result = HANDLER.handleConflict(new DataIntegrityViolationException("Message!"));

		assertThat(result.getStatusCode(), is(HttpStatus.CONFLICT));
	}

	/**
	 * @see DATAREST-706
	 */
	@Test
	public void forwardsExceptionForMiscellaneousFailure() {

		String message = "My Message!";

		ResponseEntity<ExceptionMessage> result = HANDLER.handleMiscFailures(new Exception(message));

		assertThat(result.getBody(), is(notNullValue()));
		assertThat(result.getBody().getMessage(), is(message));
	}
}
