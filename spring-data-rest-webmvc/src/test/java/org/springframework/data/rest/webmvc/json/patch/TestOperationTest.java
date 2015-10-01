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
package org.springframework.data.rest.webmvc.json.patch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TestOperationTest {

	@Test
	public void testPropertyValueEquals() throws Exception {
		// initial Todo list
		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));
		
		TestOperation test = new TestOperation("/0/complete", false);
		test.perform(todos, Todo.class);

		TestOperation test2 = new TestOperation("/1/complete", true);
		test2.perform(todos, Todo.class);

	}

	@Test(expected=PatchException.class)
	public void testPropertyValueNotEquals() throws Exception {
		// initial Todo list
		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));
		
		TestOperation test = new TestOperation("/0/complete", true);
		test.perform(todos, Todo.class);
	}
	
	@Test
	public void testListElementEquals() throws Exception {
		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));
		
		TestOperation test = new TestOperation("/1", new Todo(2L, "B", true));
		test.perform(todos, Todo.class);

	}

}
