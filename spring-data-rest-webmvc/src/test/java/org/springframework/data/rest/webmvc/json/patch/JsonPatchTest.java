/*
 * Copyright 2014 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonPatchTest {

	@Test
	public void manySuccessfulOperations() throws Exception {
		// initial Todo list
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
		// initial Todo list
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

		// nothing should have changed
		assertEquals(6, todos.size());
		assertFalse(todos.get(1).isComplete());
		assertEquals("D", todos.get(3).getDescription());
		assertEquals("E", todos.get(4).getDescription());
		assertEquals("F", todos.get(5).getDescription());
	}

	@Test
	public void failureInMiddle() throws Exception {
		// initial Todo list
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

		// nothing should have changed
		assertEquals(6, todos.size());
		assertFalse(todos.get(1).isComplete());
		assertEquals("D", todos.get(3).getDescription());
		assertEquals("E", todos.get(4).getDescription());
		assertEquals("F", todos.get(5).getDescription());
	}

	private Patch readJsonPatch(String jsonPatchFile) throws IOException, JsonParseException, JsonMappingException {
		ClassPathResource resource = new ClassPathResource(jsonPatchFile, getClass());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readValue(resource.getInputStream(), JsonNode.class);
		Patch patch = new JsonPatchPatchConverter().convert(node);
		return patch;
	}

}
