package org.springframework.data.rest.webmvc.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.validation.FieldError;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jon Brisbin
 */
public class RepositoryConstraintViolationExceptionMessage {

	private final List<ValidationError> errors = new ArrayList<ValidationError>();

	public RepositoryConstraintViolationExceptionMessage(RepositoryConstraintViolationException violationException,
			MessageSource msgSrc, Locale locale) {

		for (FieldError fe : violationException.getErrors().getFieldErrors()) {
			List<Object> args = new ArrayList<Object>();
			args.add(fe.getObjectName());
			args.add(fe.getField());
			args.add(fe.getRejectedValue());
			if (null != fe.getArguments()) {
				for (Object o : fe.getArguments()) {
					args.add(o);
				}
			}

			String msg = msgSrc.getMessage(fe.getCode(), args.toArray(), fe.getDefaultMessage(), locale);
			this.errors.add(new ValidationError(fe.getObjectName(), msg, String.format("%s", fe.getRejectedValue()), fe
					.getField()));
		}
	}

	@JsonProperty("errors")
	public List<ValidationError> getErrors() {
		return errors;
	}

	public static class ValidationError {
		String entity;
		String message;
		String invalidValue;
		String property;

		public ValidationError(String entity, String message, String invalidValue, String property) {
			this.entity = entity;
			this.message = message;
			this.invalidValue = invalidValue;
			this.property = property;
		}

		public String getEntity() {
			return entity;
		}

		public String getMessage() {
			return message;
		}

		public String getInvalidValue() {
			return invalidValue;
		}

		public String getProperty() {
			return property;
		}
	}

}
