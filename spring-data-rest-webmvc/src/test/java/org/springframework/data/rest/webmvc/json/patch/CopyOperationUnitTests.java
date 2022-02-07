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

class CopyOperationUnitTests {

	@Test
	void copyBooleanPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/0/complete").to("/1/complete");
		copy.perform(todos, Todo.class);

		assertThat(todos.get(1).isComplete()).isTrue();
	}

	@Test
	void copyStringPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/0/description").to("/1/description");
		copy.perform(todos, Todo.class);

		assertThat(todos.get(1).getDescription()).isEqualTo("A");
	}

	@Test
	void copyBooleanPropertyValueIntoStringProperty() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/0/complete").to("/1/description");
		copy.perform(todos, Todo.class);

		assertThat(todos.get(1).getDescription()).isEqualTo("true");
	}

	@Test
	void copyListElementToBeginningOfList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/1").to("/0");
		copy.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(4);
		assertThat(todos.get(0).getId().longValue()).isEqualTo(2L); // NOTE: This could be problematic if you try to save it
																																// to a DB
		// because there'll be duplicate IDs
		assertThat(todos.get(0).getDescription()).isEqualTo("B");
		assertThat(todos.get(0).isComplete()).isTrue();
	}

	@Test
	void copyListElementToMiddleOfList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/0").to("/2");
		copy.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(4);
		assertThat(todos.get(2).getId().longValue()).isEqualTo(1L); // NOTE: This could be problematic if you try to save it
																																// to a DB
		// because there'll be duplicate IDs
		assertThat(todos.get(2).getDescription()).isEqualTo("A");
		assertThat(todos.get(2).isComplete()).isTrue();
	}

	@Test
	void copyListElementToEndOfList_usingIndex() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/0").to("/3");
		copy.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(4);
		assertThat(todos.get(3).getId().longValue()).isEqualTo(1L); // NOTE: This could be problematic if you try to save it
																																// to a DB
		// because there'll be duplicate IDs
		assertThat(todos.get(3).getDescription()).isEqualTo("A");
		assertThat(todos.get(3).isComplete()).isTrue();
	}

	@Test
	void copyListElementToEndOfList_usingDash() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/0").to("/-");
		copy.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(4);
		assertThat(todos.get(3)).isEqualTo(new Todo(1L, "A", true)); // NOTE: This could be problematic if you try to save
																																	// it to a DB because there'll be duplicate IDs
	}

	@Test
	void copyListElementFromEndOfList_usingDash() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		CopyOperation copy = CopyOperation.from("/-").to("/0");
		copy.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(4);
		assertThat(todos.get(0)).isEqualTo(new Todo(3L, "C", false)); // NOTE: This could be problematic if you try to save
																																	// it to a DB because there'll be duplicate IDs
	}
}
