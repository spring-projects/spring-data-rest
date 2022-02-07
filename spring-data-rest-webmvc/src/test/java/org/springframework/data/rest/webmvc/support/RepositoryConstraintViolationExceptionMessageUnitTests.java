/*
 * Copyright 2016-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.support.RepositoryConstraintViolationExceptionMessage.ValidationError;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

/**
 * Unit tests for {@link RepositoryConstraintViolationExceptionMessage}
 *
 * @author Oliver Gierke
 */
class RepositoryConstraintViolationExceptionMessageUnitTests {

	MessageSourceAccessor accessor;
	RepositoryConstraintViolationException exception;

	@BeforeEach
	void setUp() {

		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("code", Locale.ENGLISH, "message");

		this.accessor = new MessageSourceAccessor(messageSource, Locale.ENGLISH);
		this.exception = new RepositoryConstraintViolationException(new MapBindingResult(Collections.emptyMap(), "object"));
	}

	@Test
	void rejectsNullException() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new RepositoryConstraintViolationExceptionMessage(null, accessor));
	}

	@Test
	void rejectsNullAccessor() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new RepositoryConstraintViolationExceptionMessage(exception, null));
	}

	@Test
	void calidationErrorsCaptureRejectedValueAsIs() {

		assertRejectedValue("stringValue", "string");
		assertRejectedValue("intValue", 1);
		assertRejectedValue("nullValue", null);
	}

	private void assertRejectedValue(String key, Object value) {

		Map<String, Object> map = new HashMap<String, Object>();
		map.put(key, value);

		Errors errors = new MapBindingResult(map, "object");
		errors.rejectValue(key, "code");

		RepositoryConstraintViolationExceptionMessage message = new RepositoryConstraintViolationExceptionMessage(
				new RepositoryConstraintViolationException(errors), accessor);

		List<ValidationError> result = message.getErrors();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getInvalidValue()).isEqualTo(value);
	}
}
