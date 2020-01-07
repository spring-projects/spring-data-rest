/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.rest.tests.mongodb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.springframework.util.Assert;

/**
 * Test helper methods.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class TestUtils {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Returns the given {@link String} as {@link InputStream}.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	public static InputStream asStream(String source) {
		Assert.notNull(source, "Source string must not be null!");
		return new ByteArrayInputStream(source.getBytes(UTF8));
	}
}
