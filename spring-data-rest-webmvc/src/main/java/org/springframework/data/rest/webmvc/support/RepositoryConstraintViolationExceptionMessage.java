/*
 * Copyright 2012-2016 the original author or authors.
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

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.util.Assert;
import org.springframework.validation.FieldError;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryConstraintViolationExceptionMessage {

	private final List<ValidationError> errors = new ArrayList<ValidationError>();

	/**
	 * Creates a new {@link RepositoryConstraintViolationExceptionMessage} for the given
	 * {@link RepositoryConstraintViolationException} and {@link MessageSourceAccessor}.
	 * 
	 * @param exception must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 */
	public RepositoryConstraintViolationExceptionMessage(RepositoryConstraintViolationException exception,
			MessageSourceAccessor accessor) {

		Assert.notNull(exception, "RepositoryConstraintViolationException must not be null!");
		Assert.notNull(accessor, "MessageSourceAccessor must not be null!");

		for (FieldError fieldError : exception.getErrors().getFieldErrors()) {
			this.errors.add(ValidationError.of(fieldError.getObjectName(), fieldError.getField(),
					fieldError.getRejectedValue(), accessor.getMessage(fieldError)));
		}
	}

	@JsonProperty("errors")
	public List<ValidationError> getErrors() {
		return errors;
	}

	@Value(staticConstructor = "of")
	public static class ValidationError {
		String entity, property;
		Object invalidValue;
		String message;
	}
}
