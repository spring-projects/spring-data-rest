/*
 * Copyright 2014-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TestOperationUnitTests {

	@Test
	void testPropertyValueEquals() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));

		TestOperation test = TestOperation.whetherValueAt("/0/complete").hasValue(false);
		test.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		TestOperation test2 = TestOperation.whetherValueAt("/1/complete").hasValue(true);
		test2.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

	}

	@Test
	void testPropertyValueNotEquals() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));

		TestOperation test = TestOperation.whetherValueAt("/0/complete").hasValue(true);

		assertThatExceptionOfType(PatchException.class) //
				.isThrownBy(() -> test.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE));
	}

	@Test
	void testListElementEquals() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));

		TestOperation test = TestOperation.whetherValueAt("/1").hasValue(new Todo(2L, "B", true));
		test.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);
	}
}
