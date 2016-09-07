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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
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
public class JsonPatchTests {

	@Test
	public void manySuccessfulOperations() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "D", false));
		todos.add(new Todo(5L, "E", false));
		todos.add(new Todo(6L, "F", false));

		Patch patch = readJsonPatch("patch-many-successful-operations.json");
		assertEquals(6, patch.size());

		List<Todo> patchedTodos = patch.apply(todos, Todo.class);

		assertEquals(6, todos.size());
		assertTrue(patchedTodos.get(1).isComplete());
		assertEquals("C", patchedTodos.get(3).getDescription());
		assertEquals("A", patchedTodos.get(4).getDescription());
	}

	@Test
	public void failureAtBeginning() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "D", false));
		todos.add(new Todo(5L, "E", false));
		todos.add(new Todo(6L, "F", false));

		Patch patch = readJsonPatch("patch-failing-operation-first.json");

		try {
			patch.apply(todos, Todo.class);
			fail();
		} catch (PatchException e) {
			assertEquals("Test against path '/5/description' failed.", e.getMessage());
		}

		assertEquals(6, todos.size());
		assertFalse(todos.get(1).isComplete());
		assertEquals("D", todos.get(3).getDescription());
		assertEquals("E", todos.get(4).getDescription());
		assertEquals("F", todos.get(5).getDescription());
	}

	@Test
	public void failureInMiddle() throws Exception {

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", true));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));
		todos.add(new Todo(4L, "D", false));
		todos.add(new Todo(5L, "E", false));
		todos.add(new Todo(6L, "F", false));

		Patch patch = readJsonPatch("patch-failing-operation-in-middle.json");

		try {
			patch.apply(todos, Todo.class);
			fail();
		} catch (PatchException e) {
			assertEquals("Test against path '/5/description' failed.", e.getMessage());
		}

		assertEquals(6, todos.size());
		assertFalse(todos.get(1).isComplete());
		assertEquals("D", todos.get(3).getDescription());
		assertEquals("E", todos.get(4).getDescription());
		assertEquals("F", todos.get(5).getDescription());
	}

	/**
	 * @see DATAREST-885
	 */
	@Test
	public void patchArray() throws Exception {

		Todo todo = new Todo(1L, "F", false);

		Patch patch = readJsonPatch("patch-array.json");
		assertEquals(1, patch.size());

		Todo patchedTodo = patch.apply(todo, Todo.class);
		assertEquals(Arrays.asList("one", "two", "three"), patchedTodo.getItems());
	}

	@Test
	public void patchUnknownType() throws Exception {
		Todo todo = new Todo();
		todo.setAmount(BigInteger.ONE);

		try {
			Patch patch = readJsonPatch("patch-biginteger.json");
			assertEquals(1, patch.size());
			assertEquals(new BigInteger("18446744073709551616"), patch.getOperations().get(0).value);
		} catch (PatchException e) {
			assertEquals("Unrecognized valueNode type at path: /amount. valueNode: 18446744073709551616", e.getMessage());
		}
	}

	@Test
	public void failureWithInvalidPatchContent() throws Exception {
		Todo todo = new Todo();
		todo.setDescription("Description");

		Patch patch = readJsonPatch("patch-failing-with-invalid-content.json");
		assertEquals(1, patch.size());

		try {
			Todo patchedTodo = patch.apply(todo, Todo.class);
			assertEquals("Description", patchedTodo.getDescription());
		} catch (PatchException e) {
			assertEquals("JSON deserialization exception", e.getMessage());
		}
	}

	private Patch readJsonPatch(String jsonPatchFile) throws IOException, JsonParseException, JsonMappingException {

		ClassPathResource resource = new ClassPathResource(jsonPatchFile, getClass());
		JsonNode node = new ObjectMapper().readValue(resource.getInputStream(), JsonNode.class);
		Patch patch = new JsonPatchPatchConverter(new ObjectMapper()).convert(node);

		return patch;
	}
}
