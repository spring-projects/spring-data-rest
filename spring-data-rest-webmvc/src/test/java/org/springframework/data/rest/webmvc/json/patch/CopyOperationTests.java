/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CopyOperationTests {

	@Test
	public void copyBooleanPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/1/complete", "/0/complete");
		copy.perform(todos, Todo.class);

		assertTrue(todos.get(1).isComplete());
	}

	@Test
	public void copyStringPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/1/description", "/0/description");
		copy.perform(todos, Todo.class);

		assertEquals("A", todos.get(1).getDescription());
	}

	@Test
	public void copyBooleanPropertyValueIntoStringProperty() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/1/description", "/0/complete");
		copy.perform(todos, Todo.class);

		assertEquals("true", todos.get(1).getDescription());
	}

	@Test
	public void copyListElementToBeginningOfList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/0", "/1");
		copy.perform(todos, Todo.class);

		assertEquals(4, todos.size());
		assertEquals(2L, todos.get(0).getId().longValue()); // NOTE: This could be problematic if you try to save it to a DB
																												// because there'll be duplicate IDs
		assertEquals("B", todos.get(0).getDescription());
		assertTrue(todos.get(0).isComplete());
	}

	@Test
	public void copyListElementToMiddleOfList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/2", "/0");
		copy.perform(todos, Todo.class);

		assertEquals(4, todos.size());
		assertEquals(1L, todos.get(2).getId().longValue()); // NOTE: This could be problematic if you try to save it to a DB
																												// because there'll be duplicate IDs
		assertEquals("A", todos.get(2).getDescription());
		assertTrue(todos.get(2).isComplete());
	}

	@Test
	public void copyListElementToEndOfList_usingIndex() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/3", "/0");
		copy.perform(todos, Todo.class);

		assertEquals(4, todos.size());
		assertEquals(1L, todos.get(3).getId().longValue()); // NOTE: This could be problematic if you try to save it to a DB
																												// because there'll be duplicate IDs
		assertEquals("A", todos.get(3).getDescription());
		assertTrue(todos.get(3).isComplete());
	}

	@Test
	public void copyListElementToEndOfList_usingTilde() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/~", "/0");
		copy.perform(todos, Todo.class);

		assertEquals(4, todos.size());
		assertEquals(new Todo(1L, "A", true), todos.get(3)); // NOTE: This could be problematic if you try to save it to a
																													// DB because there'll be duplicate IDs
	}

	@Test
	public void copyListElementFromEndOfList_usingTilde() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = new CopyOperation("/0", "/~");
		copy.perform(todos, Todo.class);

		assertEquals(4, todos.size());
		assertEquals(new Todo(3L, "C", false), todos.get(0)); // NOTE: This could be problematic if you try to save it to a
																													// DB because there'll be duplicate IDs
	}
}
