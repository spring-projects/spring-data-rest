/*
 * Copyright 2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * General unit tests for {@link PatchOperation} implementations.
 * 
 * @author Oliver Gierke
 */
public class PatchOperationUnitTests {

	@Test // DATAREST-1137
	public void invalidPathGetsRejected() {

		String invalidPath = "/nonExistant";

		verifyIllegalPath(new AddOperation(invalidPath, null));
		verifyIllegalPath(new CopyOperation(invalidPath, null));
		verifyIllegalPath(new MoveOperation(invalidPath, null));
		verifyIllegalPath(new RemoveOperation(invalidPath));
		verifyIllegalPath(new ReplaceOperation(invalidPath, null));
		verifyIllegalPath(new TestOperation(invalidPath, null));
	}

	private static void verifyIllegalPath(PatchOperation operation) {

		Todo todo = new Todo(1L, "A", false);

		assertThatExceptionOfType(PatchException.class) //
				.isThrownBy(() -> operation.perform(todo, Todo.class));
	}
}
