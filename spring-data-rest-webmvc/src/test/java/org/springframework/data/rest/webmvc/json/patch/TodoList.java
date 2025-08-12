/*
 * Copyright 2014-2025 the original author or authors.
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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

class TodoList implements Serializable {

	private static final @Serial long serialVersionUID = 1L;

	private List<Todo> todos;
	private Todo[] todoArray;
	private String name;

	public List<Todo> getTodos() {
		return this.todos;
	}

	public Todo[] getTodoArray() {
		return this.todoArray;
	}

	public String getName() {
		return this.name;
	}

	public void setTodos(List<Todo> todos) {
		this.todos = todos;
	}

	public void setTodoArray(Todo[] todoArray) {
		this.todoArray = todoArray;
	}

	public void setName(String name) {
		this.name = name;
	}
}
