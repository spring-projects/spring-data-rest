package org.springframework.data.rest.webmvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * @author Thibaud Lepretre
 */
@ControllerAdvice
public class GlobalExceptionHandler implements MessageSourceAware {

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);


	private MessageSourceAccessor messageSourceAccessor;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.MessageSourceAware#setMessageSource(org.springframework.context.MessageSource)
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
	}

	@ExceptionHandler({ NullPointerException.class })
	@ResponseBody
	public ResponseEntity<?> handleNPE(NullPointerException npe) {
		return errorResponse(npe, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler({ ResourceNotFoundException.class })
	@ResponseBody
	public ResponseEntity<?> handleNotFound() {
		return notFound();
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class })
	@ResponseBody
	public ResponseEntity<ExceptionMessage> handleNotReadable(HttpMessageNotReadableException e) {
		return badRequest(e);
	}

	/**
	 * Handle failures commonly thrown from code tries to read incoming data and convert or cast it to the right type.
	 *
	 * @param t
	 * @return
	 */
	@ExceptionHandler({ InvocationTargetException.class, IllegalArgumentException.class, ClassCastException.class,
			ConversionFailedException.class })
	@ResponseBody
	public ResponseEntity handleMiscFailures(Throwable t) {
		if (null != t.getCause() && t.getCause() instanceof ResourceNotFoundException) {
			return notFound();
		}
		return badRequest(t);
	}

	@ExceptionHandler({ RepositoryConstraintViolationException.class })
	@ResponseBody
	public ResponseEntity handleRepositoryConstraintViolationException(Locale locale,
			RepositoryConstraintViolationException rcve) {

		return response(null, new RepositoryConstraintViolationExceptionMessage(rcve, messageSourceAccessor),
				HttpStatus.BAD_REQUEST);
	}

	/**
	 * Send a 409 Conflict in case of concurrent modification.
	 *
	 * @param ex
	 * @return HTTP Status 409 ResponseEntity
	 */
	@ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
	public ResponseEntity handleConflict(Exception ex) {
		return errorResponse(null, ex, HttpStatus.CONFLICT);
	}

	/**
	 * Send {@code 405 Method Not Allowed} and include the supported {@link org.springframework.http.HttpMethod}s in the {@code Allow} header.
	 *
	 * @param o_O
	 * @return HTTP Status 405 ResponseEntity
	 */
	@ExceptionHandler
	public ResponseEntity<Void> handle(HttpRequestMethodNotSupportedException o_O) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(o_O.getSupportedHttpMethods());

		return new ResponseEntity<Void>(headers, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler
	public ResponseEntity<Void> handle(ETagDoesntMatchException o_O) {

		HttpHeaders headers = o_O.getExpectedETag().addTo(new HttpHeaders());
		return new ResponseEntity<Void>(headers, HttpStatus.PRECONDITION_FAILED);
	}

	protected <T> ResponseEntity<T> notFound() {
		return notFound(null, null);
	}

	protected <T> ResponseEntity<T> notFound(HttpHeaders headers, T body) {
		return response(headers, body, HttpStatus.NOT_FOUND);
	}

	protected <T extends Throwable> ResponseEntity<ExceptionMessage> badRequest(T throwable) {
		return badRequest(null, throwable);
	}

	protected <T extends Throwable> ResponseEntity<ExceptionMessage> badRequest(HttpHeaders headers, T throwable) {
		return errorResponse(headers, throwable, HttpStatus.BAD_REQUEST);
	}

	public <T extends Throwable> ResponseEntity<ExceptionMessage> errorResponse(T throwable, HttpStatus status) {
		return errorResponse(null, throwable, status);
	}

	public <T extends Throwable> ResponseEntity<ExceptionMessage> errorResponse(HttpHeaders headers, T throwable,
			HttpStatus status) {
		if (null != throwable && null != throwable.getMessage()) {
			LOG.error(throwable.getMessage(), throwable);
			return response(headers, new ExceptionMessage(throwable), status);
		} else {
			return response(headers, null, status);
		}
	}

	public <T> ResponseEntity<T> response(HttpHeaders headers, T body, HttpStatus status) {
		HttpHeaders hdrs = new HttpHeaders();
		if (null != headers) {
			hdrs.putAll(headers);
		}
		return new ResponseEntity<T>(body, hdrs, status);
	}
}
