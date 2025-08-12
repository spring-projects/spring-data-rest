/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.Objects;

import org.jspecify.annotations.Nullable;

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

		Assert.notNull(exception, "RepositoryConstraintViolationException must not be null");
		Assert.notNull(accessor, "MessageSourceAccessor must not be null");

		for (FieldError fieldError : exception.getErrors().getFieldErrors()) {
			this.errors.add(ValidationError.of(fieldError.getObjectName(), fieldError.getField(),
					fieldError.getRejectedValue(), accessor.getMessage(fieldError)));
		}
	}

	@JsonProperty("errors")
	public List<ValidationError> getErrors() {
		return errors;
	}

	public static final class ValidationError {

		private final String entity;
		private final String property;
		private final @Nullable Object invalidValue;
		private final String message;

		private ValidationError(String entity, String property, @Nullable Object invalidValue, String message) {

			Assert.hasText(entity, "Entity must not be null or empty");
			Assert.hasText(property, "Property must not be null or empty");
			Assert.hasText(message, "Message must not be null or empty");

			this.entity = entity;
			this.property = property;
			this.invalidValue = invalidValue;
			this.message = message;
		}

		public static ValidationError of(String entity, String property, @Nullable Object invalidValue, String message) {
			return new ValidationError(entity, property, invalidValue, message);
		}

		public String getEntity() {
			return this.entity;
		}

		public String getProperty() {
			return this.property;
		}

		@Nullable
		public Object getInvalidValue() {
			return this.invalidValue;
		}

		public String getMessage() {
			return this.message;
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (o == this) {
				return true;
			}

			if (!(o instanceof ValidationError)) {
				return false;
			}

			ValidationError other = (ValidationError) o;

			return Objects.equals(entity, other.entity) //
					&& Objects.equals(property, other.property) //
					&& Objects.equals(invalidValue, other.invalidValue) //
					&& Objects.equals(message, other.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(entity, property, invalidValue, message);
		}

		@Override
		public java.lang.String toString() {
			return "RepositoryConstraintViolationExceptionMessage.ValidationError(entity=" + entity + ", property=" + property
					+ ", invalidValue=" + invalidValue + ", message=" + message + ")";
		}
	}
}
