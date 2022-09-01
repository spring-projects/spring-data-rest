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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JsonPatchPatchConverter}.
 *
 * @author Craig Walls
 * @author Oliver Gierke
 * @author Mathias Düsterhöft
 * @author Oliver Trosien
 */
class JsonPatchUnitTests {

	@Test
	void manySuccessfulOperations() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "D", false));
		todos.add(new Todo(5L, "E", false));
		todos.add(new Todo(6L, "F", false));

		Patch patch = readJsonPatch("patch-many-successful-operations.json");
		assertThat(patch.size()).isEqualTo(6);

		List<Todo> patchedTodos = patch.apply(todos, Todo.class);

		assertThat(todos.size()).isEqualTo(6);
		assertThat(patchedTodos.get(1).isComplete()).isTrue();
		assertThat(patchedTodos.get(3).getDescription()).isEqualTo("C");
		assertThat(patchedTodos.get(4).getDescription()).isEqualTo("A");
	}

	@Test
	void failureAtBeginning() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "D", false));
		todos.add(new Todo(5L, "E", false));
		todos.add(new Todo(6L, "F", false));

		Patch patch = readJsonPatch("patch-failing-operation-first.json");

		assertThatExceptionOfType(PatchException.class)
				.isThrownBy(() -> patch.apply(todos, Todo.class))
				.withMessage("Test against path '/5/description' failed.");

		assertThat(todos.size()).isEqualTo(6);
		assertThat(todos.get(1).isComplete()).isFalse();
		assertThat(todos.get(3).getDescription()).isEqualTo("D");
		assertThat(todos.get(4).getDescription()).isEqualTo("E");
		assertThat(todos.get(5).getDescription()).isEqualTo("F");
	}

	@Test
	void failureInMiddle() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "D", false));
		todos.add(new Todo(5L, "E", false));
		todos.add(new Todo(6L, "F", false));

		Patch patch = readJsonPatch("patch-failing-operation-in-middle.json");

		assertThatExceptionOfType(PatchException.class)
				.isThrownBy(() -> patch.apply(todos, Todo.class))
				.withMessage("Test against path '/5/description' failed.");

		assertThat(todos.size()).isEqualTo(6);
		assertThat(todos.get(1).isComplete()).isFalse();
		assertThat(todos.get(3).getDescription()).isEqualTo("D");
		assertThat(todos.get(4).getDescription()).isEqualTo("E");
		assertThat(todos.get(5).getDescription()).isEqualTo("F");
	}

	@Test // DATAREST-889
	void patchArray() throws Exception {

		Todo todo = new Todo(1L, "F", false);

		Patch patch = readJsonPatch("patch-array.json");
		assertThat(patch.size()).isEqualTo(1);

		Todo patchedTodo = patch.apply(todo, Todo.class);
		assertThat(patchedTodo.getItems()).contains("one", "two", "three");
	}

	@Test // DATAREST-889
	void patchUnknownType() {

		Todo todo = new Todo();
		todo.setAmount(BigInteger.ONE);

		assertThatExceptionOfType(PatchException.class)
				.isThrownBy(() -> readJsonPatch("patch-biginteger.json"))
				.withMessageContaining("/amount")
				.withMessageContaining("18446744073709551616");
	}

	@Test // DATAREST-889
	void failureWithInvalidPatchContent() throws Exception {

		Todo todo = new Todo();
		todo.setDescription("Description");

		Patch patch = readJsonPatch("patch-failing-with-invalid-content.json");

		assertThatExceptionOfType(PatchException.class) //
				.isThrownBy(() -> patch.apply(todo, Todo.class)) //
				.withMessageContaining("content") //
				.withMessageContaining("blabla") //
				.withMessageContaining(String.class.getName().toString());
	}

	@Test // DATAREST-1127
	void rejectsInvalidPaths() {

		assertThatExceptionOfType(PatchException.class).isThrownBy(() -> {
			readJsonPatch("patch-invalid-path.json").apply(new Todo(), Todo.class);
		});
	}

	private Patch readJsonPatch(String jsonPatchFile) throws IOException, JsonParseException, JsonMappingException {

		ClassPathResource resource = new ClassPathResource(jsonPatchFile, getClass());
		JsonNode node = new ObjectMapper().readValue(resource.getInputStream(), JsonNode.class);
		Patch patch = new JsonPatchPatchConverter(new ObjectMapper(), TestPropertyPathContext.INSTANCE).convert(node);

		return patch;
	}
}
