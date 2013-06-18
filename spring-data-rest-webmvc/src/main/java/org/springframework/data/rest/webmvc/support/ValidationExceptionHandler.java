package org.springframework.data.rest.webmvc.support;

import java.util.Locale;
import javax.validation.ConstraintViolationException;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
public class ValidationExceptionHandler {

	public ResponseEntity<?> handleValidationException(RuntimeException ex, MessageSource msgsrc, Locale locale) {
		Assert.isAssignable(ConstraintViolationException.class, ex.getClass());
		return new ResponseEntity<ConstraintViolationExceptionMessage>(new ConstraintViolationExceptionMessage(
				(ConstraintViolationException) ex, msgsrc, locale), HttpStatus.BAD_REQUEST

		);
	}

}
