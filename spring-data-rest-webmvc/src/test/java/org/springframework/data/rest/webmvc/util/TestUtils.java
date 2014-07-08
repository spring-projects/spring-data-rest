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
package org.springframework.data.rest.webmvc.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Scanner;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.rest.webmvc.jpa.JpaWebTests;
import org.springframework.util.Assert;

/**
 * Test helper methods.
 * 
 * @author Oliver Gierke
 */
public class TestUtils {

	public static String readFileFromClasspath(String name) throws Exception {

		ClassPathResource file = new ClassPathResource(name, JpaWebTests.class);
		StringBuilder builder = new StringBuilder();

		Scanner scanner = new Scanner(file.getFile(), "UTF-8");

		try {

			while (scanner.hasNextLine()) {
				builder.append(scanner.nextLine());
			}

		} finally {
			scanner.close();
		}

		return builder.toString();
	}

	/**
	 * Returns the given {@link String} as {@link InputStream}.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	public static InputStream asStream(String source) {
		Assert.notNull(source, "Source string must not be null!");
		return new ByteArrayInputStream(source.getBytes(Charset.forName("UTF-8")));
	}
}
