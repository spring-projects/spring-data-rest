/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.data.rest.webmvc.json.patch.SpelPath.UntypedSpelPath;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * <p>
 * Operation to test values on a given target.
 * </p>
 * <p>
 * If the value given matches the value given at the path, the operation completes as a no-op. On the other hand, if the
 * values do not match or if there are any errors interpreting the path, a {@link PatchException} will be thrown.
 * </p>
 *
 * @author Craig Walls
 * @author Oliver Gierke
 */
class TestOperation extends PatchOperation {

	/**
	 * Constructs the test operation
	 *
	 * @param path The path to test. (e.g., '/foo/bar/4')
	 * @param value The value to test the path against.
	 */
	private TestOperation(UntypedSpelPath path, Object value) {
		super("test", path, value);
	}

	public static TestOperationBuilder whetherValueAt(String path) {
		return new TestOperationBuilder(path);
	}

	static class TestOperationBuilder {

		private final String path;

		private TestOperationBuilder(String path) {

			Assert.hasText(path, "Path must not be null or empty");

			this.path = path;
		}

		public TestOperation hasValue(Object value) {
			return new TestOperation(SpelPath.untyped(path), value);
		}
	}

	@Override
	void perform(Object target, Class<?> type, BindContext context) {

		Object expected = normalizeIfNumber(evaluateValueFromTarget(target, type, context));
		Object actual = normalizeIfNumber(path.bindForRead(type, context).getValue(target));

		if (!ObjectUtils.nullSafeEquals(expected, actual)) {
			throw new PatchException("Test against path '" + path + "' failed");
		}
	}

	private static Object normalizeIfNumber(Object expected) {

		if (expected instanceof Double || expected instanceof Float) {
			expected = BigDecimal.valueOf(((Number) expected).doubleValue());
		} else if (expected instanceof Number) {
			expected = BigInteger.valueOf(((Number) expected).longValue());
		}

		return expected;
	}
}
