/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.util;

import java.nio.charset.Charset;
import java.util.Scanner;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.rest.webmvc.jpa.JpaWebTests;

/**
 * Test helper methods.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class TestUtils {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static String readFileFromClasspath(String name) throws Exception {

		ClassPathResource file = new ClassPathResource(name, JpaWebTests.class);
		StringBuilder builder = new StringBuilder();

		Scanner scanner = new Scanner(file.getFile(), UTF8.name());

		try {

			while (scanner.hasNextLine()) {
				builder.append(scanner.nextLine());
			}

		} finally {
			scanner.close();
		}

		return builder.toString();
	}
}
