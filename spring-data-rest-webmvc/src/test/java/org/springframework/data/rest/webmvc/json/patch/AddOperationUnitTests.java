/*
 * Copyright 2014-2023 the original author or authors.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AddOperationUnitTests {

	@Test
	void addBooleanPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		AddOperation add = AddOperation.of("/1/complete", true);
		add.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos.get(1).isComplete()).isTrue();
	}

	@Test
	void addStringPropertyValue() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		AddOperation add = AddOperation.of("/1/description", "BBB");
		add.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos.get(1).getDescription()).isEqualTo("BBB");
	}

	@Test
	void addItemToList() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		AddOperation add = AddOperation.of("/1", new Todo(null, "D", true));
		add.perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos.size()).isEqualTo(4);
		assertThat(todos.get(0).getDescription()).isEqualTo("A");
		assertThat(todos.get(0).isComplete()).isFalse();
		assertThat(todos.get(1).getDescription()).isEqualTo("D");
		assertThat(todos.get(1).isComplete()).isTrue();
		assertThat(todos.get(2).getDescription()).isEqualTo("B");
		assertThat(todos.get(2).isComplete()).isFalse();
		assertThat(todos.get(3).getDescription()).isEqualTo("C");
		assertThat(todos.get(3).isComplete()).isFalse();
	}

	@Test // DATAREST-995
	void addsItemsToNestedList() {

		Todo todo = new Todo(1L, "description", false);

		AddOperation.of("/items/-", "Some text.").perform(todo, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todo.getItems().get(0)).isEqualTo("Some text.");
	}

	@Test // DATAREST-1039
	void addsLazilyEvaluatedObjectToList() throws Exception {

		Todo todo = new Todo(1L, "description", false);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree("\"Some text.\"");
		JsonLateObjectEvaluator evaluator = new JsonLateObjectEvaluator(mapper, node);

		AddOperation.of("/items/-", evaluator).perform(todo, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todo.getItems().get(0)).isEqualTo("Some text.");
	}

	@Test // DATAREST-1039
	void initializesNullCollectionsOnAppend() {

		Todo todo = new Todo(1L, "description", false);

		AddOperation.of("/uninitialized/-", "Text").perform(todo, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todo.getUninitialized()).containsExactly("Text");
	}

	@Test // DATAREST-1273
	void addsItemToTheEndOfACollectionViaIndex() {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));

		Todo todo = new Todo(2L, "B", true);
		AddOperation.of("/1", todo).perform(todos, Todo.class, TestPropertyPathContext.INSTANCE);

		assertThat(todos).element(1).isEqualTo(todo);
	}

	@Test // DATAREST-1273
	void rejectsAdditionBeyondEndOfList() {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));

		assertThatExceptionOfType(PatchException.class) //
				.isThrownBy(() -> AddOperation.of("/2", new Todo(2L, "B", true)).perform(todos, Todo.class,
						TestPropertyPathContext.INSTANCE)) //
				.withMessageContaining("index") //
				.withMessageContaining("2") //
				.withMessageContaining("1");
	}

	@Test // DATAREST-1479
	void manipulatesNestedCollectionProperly() {

		List<Todo> todos = new ArrayList<>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));

		TodoList todoList = new TodoList();
		todoList.setTodos(todos);
		TodoListWrapper outer = new TodoListWrapper(todoList);

		Todo newTodo = new Todo(3L, "C", false);
		AddOperation.of("/todoList/todos/-", newTodo).perform(outer, TodoListWrapper.class,
				TestPropertyPathContext.INSTANCE);

		assertThat(outer.todoList.getTodos()).containsExactly(todos.get(0), todos.get(1), newTodo);
	}

	public static class TodoListWrapper {
		public TodoList todoList;

		public TodoListWrapper(TodoList todoList) {
			this.todoList = todoList;
		}

		public TodoListWrapper() {}

		public TodoList getTodoList() {
			return this.todoList;
		}

		public void setTodoList(TodoList todoList) {
			this.todoList = todoList;
		}
	}
}
