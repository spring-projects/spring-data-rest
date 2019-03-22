/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.validation.FieldError;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryConstraintViolationExceptionMessage {

	private final List<ValidationError> errors = new ArrayList<ValidationError>();

	public RepositoryConstraintViolationExceptionMessage(RepositoryConstraintViolationException violationException,
			MessageSourceAccessor accessor) {

		for (FieldError fieldError : violationException.getErrors().getFieldErrors()) {

			String message = accessor.getMessage(fieldError);

			this.errors.add(new ValidationError(fieldError.getObjectName(), message, String.format("%s",
					fieldError.getRejectedValue()), fieldError.getField()));
		}
	}

	@JsonProperty("errors")
	public List<ValidationError> getErrors() {
		return errors;
	}

	public static class ValidationError {

		private final String entity;
		private final String message;
		private final String invalidValue;
		private final String property;

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
