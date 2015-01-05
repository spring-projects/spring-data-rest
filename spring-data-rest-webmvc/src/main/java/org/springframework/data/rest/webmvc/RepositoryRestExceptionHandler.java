/*
 * Copyright 2014 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.support.ETagDoesntMatchException;
import org.springframework.data.rest.webmvc.support.ExceptionMessage;
import org.springframework.data.rest.webmvc.support.RepositoryConstraintViolationExceptionMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handler for Spring Data REST controllers.
 * 
 * @author Thibaud Lepretre
 * @author Oliver Gierke
 */
@ControllerAdvice(basePackageClasses = RepositoryRestExceptionHandler.class)
public class RepositoryRestExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryRestExceptionHandler.class);

	private final MessageSourceAccessor messageSourceAccessor;

	/**
	 * Creates a new {@link RepositoryRestExceptionHandler} using the given {@link MessageSource}.
	 * 
	 * @param messageSource must not be {@literal null}.
	 */
	public RepositoryRestExceptionHandler(MessageSource messageSource) {

		Assert.notNull(messageSource, "MessageSource must not be null!");
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
	}

	/**
	 * Handles {@link ResourceNotFoundException} by returning {@code 404 Not Found}.
	 * 
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler
	ResponseEntity<?> handleNotFound(ResourceNotFoundException o_O) {
		return notFound();
	}

	/**
	 * Handles {@link HttpMessageNotReadableException} by returning {@code 400 Bad Request}.
	 * 
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler
	ResponseEntity<ExceptionMessage> handleNotReadable(HttpMessageNotReadableException o_O) {
		return badRequest(o_O);
	}

	/**
	 * Handle failures commonly thrown from code tries to read incoming data and convert or cast it to the right type by
	 * returning {@code 500 Internal Server Error} and the thrown exception marshalled into JSON.
	 *
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler({ InvocationTargetException.class, IllegalArgumentException.class, ClassCastException.class,
			ConversionFailedException.class, NullPointerException.class })
	ResponseEntity<ExceptionMessage> handleMiscFailures(Exception o_O) {
		return errorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Handles {@link RepositoryConstraintViolationException}s by returning {@code 400 Bad Request}.
	 * 
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler
	ResponseEntity<RepositoryConstraintViolationExceptionMessage> handleRepositoryConstraintViolationException(
			RepositoryConstraintViolationException o_O) {

		return response(new HttpHeaders(), new RepositoryConstraintViolationExceptionMessage(o_O, messageSourceAccessor),
				HttpStatus.BAD_REQUEST);
	}

	/**
	 * Send a {@code 409 Conflict} in case of concurrent modification.
	 *
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
	ResponseEntity<ExceptionMessage> handleConflict(Exception o_O) {
		return errorResponse(null, o_O, HttpStatus.CONFLICT);
	}

	/**
	 * Send {@code 405 Method Not Allowed} and include the supported {@link org.springframework.http.HttpMethod}s in the
	 * {@code Allow} header.
	 *
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler
	ResponseEntity<Void> handle(HttpRequestMethodNotSupportedException o_O) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(o_O.getSupportedHttpMethods());

		return new ResponseEntity<Void>(headers, HttpStatus.METHOD_NOT_ALLOWED);
	}

	/**
	 * Handles {@link ETagDoesntMatchException} by returning {@code 412 Precondition Failed}.
	 * 
	 * @param o_O the exception to handle.
	 * @return
	 */
	@ExceptionHandler
	ResponseEntity<Void> handle(ETagDoesntMatchException o_O) {

		HttpHeaders headers = o_O.getExpectedETag().addTo(new HttpHeaders());
		return new ResponseEntity<Void>(headers, HttpStatus.PRECONDITION_FAILED);
	}

	private <T> ResponseEntity<T> notFound() {
		return notFound(new HttpHeaders(), null);
	}

	private <T> ResponseEntity<T> notFound(HttpHeaders headers, T body) {
		return response(headers, body, HttpStatus.NOT_FOUND);
	}

	private <T extends Exception> ResponseEntity<ExceptionMessage> badRequest(T throwable) {
		return badRequest(new HttpHeaders(), throwable);
	}

	private <T extends Exception> ResponseEntity<ExceptionMessage> badRequest(HttpHeaders headers, T throwable) {
		return errorResponse(headers, throwable, HttpStatus.BAD_REQUEST);
	}

	private <T extends Exception> ResponseEntity<ExceptionMessage> errorResponse(T throwable, HttpStatus status) {
		return errorResponse(new HttpHeaders(), throwable, status);
	}

	private <T extends Exception> ResponseEntity<ExceptionMessage> errorResponse(HttpHeaders headers,
			Exception exception, HttpStatus status) {

		if (null != exception && null != exception.getMessage()) {

			LOG.error(exception.getMessage(), exception);

			return response(headers, new ExceptionMessage(exception), status);

		} else {

			return response(headers, null, status);
		}
	}

	public <T> ResponseEntity<T> response(HttpHeaders headers, T body, HttpStatus status) {

		Assert.notNull(headers, "Headers must not be null!");
		Assert.notNull(status, "HttpStatus must not be null!");

		return new ResponseEntity<T>(body, headers, status);
	}
}
