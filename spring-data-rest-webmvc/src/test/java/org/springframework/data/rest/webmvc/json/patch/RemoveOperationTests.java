/*
 * Copyright 2014-2021 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RemoveOperationTests {

	@Test
	public void removePropertyFromObject() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		RemoveOperation.valueAt("/1/description").perform(todos, Todo.class);

		assertNull(todos.get(1).getDescription());
	}

	@Test
	public void removeItemFromList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		RemoveOperation.valueAt("/1").perform(todos, Todo.class);

		assertEquals(2, todos.size());
		assertEquals("A", todos.get(0).getDescription());
		assertEquals("C", todos.get(1).getDescription());
	}

}
