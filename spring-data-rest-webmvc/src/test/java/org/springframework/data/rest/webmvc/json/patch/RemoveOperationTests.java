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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RemoveOperationTests {

	@Test
	void removePropertyFromObject() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		RemoveOperation.valueAt("/1/description").perform(todos, Todo.class);

		assertThat(todos.get(1).getDescription()).isNull();
	}

	@Test
	void removeItemFromList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		RemoveOperation.valueAt("/1").perform(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(2);
		assertThat(todos.get(0).getDescription()).isEqualTo("A");
		assertThat(todos.get(1).getDescription()).isEqualTo("C");
	}
}
