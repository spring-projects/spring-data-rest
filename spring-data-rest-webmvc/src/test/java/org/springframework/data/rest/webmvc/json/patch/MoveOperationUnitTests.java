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

class MoveOperationUnitTests {

	@Test
	void moveBooleanPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		MoveOperation move = MoveOperation.from("/0/complete").to("/1/complete");

		assertThatExceptionOfType(PatchException.class)
				.isThrownBy(() -> move.perform(todos, Todo.class))
				.withMessage("Path '/0/complete' is not nullable.");

		assertThat(todos.get(1).isComplete()).isFalse();
	}

	@Test
	void moveStringPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		MoveOperation move = MoveOperation.from("/0/description").to("/1/description");
		move.perform(todos, Todo.class);

		assertThat(todos.get(1).getDescription()).isEqualTo("A");
	}

	@Test
	void moveBooleanPropertyValueIntoStringProperty() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		MoveOperation move = MoveOperation.from("/0/complete").to("/1/description");

		assertThatExceptionOfType(PatchException.class)
				.isThrownBy(() -> move.perform(todos, Todo.class))
				.withMessage("Path '/0/complete' is not nullable.");

		assertThat(todos.get(1).getDescription()).isEqualTo("B");
	}

	//
	// NOTE: Moving an item about in a list probably has zero effect, as the order of the list is
	// usually determined by the DB query that produced the list. Moving things around in a
	// java.util.List and then saving those items really means nothing to the DB, as the
	// properties that determined the original order are still the same and will result in
	// the same order when the objects are queries again.
	//

	@Test
	void moveListElementToBeginningOfList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", true));
		todos.add(new Todo(3L, "C", false));

		MoveOperation move = MoveOperation.from("/1").to("/0");
		move.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(3);
		assertThat(todos.get(0).getId().longValue()).isEqualTo(2L);
		assertThat(todos.get(0).getDescription()).isEqualTo("B");
		assertThat(todos.get(0).isComplete()).isTrue();
	}

	@Test
	void moveListElementToMiddleOfList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		MoveOperation move = MoveOperation.from("/0").to("/2");
		move.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(3);
		assertThat(todos.get(2).getId().longValue()).isEqualTo(1L);
		assertThat(todos.get(2).getDescription()).isEqualTo("A");
		assertThat(todos.get(2).isComplete()).isTrue();
	}

	@Test
	void moveListElementToEndOfList_usingIndex() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		MoveOperation move = MoveOperation.from("/0").to("/2");
		move.perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(3);
		assertThat(todos.get(2).getId().longValue()).isEqualTo(1L);
		assertThat(todos.get(2).getDescription()).isEqualTo("A");
		assertThat(todos.get(2).isComplete()).isTrue();
	}

	@Test
	void moveListElementToBeginningOfList_usingDash() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "E", false));
		todos.add(new Todo(2L, "G", false));

		List<Todo> expected = new ArrayList<Todo>();
		expected.add(new Todo(1L, "A", true));
		expected.add(new Todo(2L, "G", false));
		expected.add(new Todo(3L, "C", false));
		expected.add(new Todo(4L, "E", false));

		MoveOperation move = MoveOperation.from("/-").to("/1");
		move.perform(todos, Todo.class);

		assertThat(todos).isEqualTo(expected);
	}

	@Test
	void moveListElementToEndOfList_usingDash() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "G", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "E", false));

		List<Todo> expected = new ArrayList<Todo>();
		expected.add(new Todo(1L, "A", true));
		expected.add(new Todo(3L, "C", false));
		expected.add(new Todo(4L, "E", false));
		expected.add(new Todo(2L, "G", false));

		MoveOperation move = MoveOperation.from("/1").to("/-");
		move.perform(todos, Todo.class);

		assertThat(todos).isEqualTo(expected);
	}
}
