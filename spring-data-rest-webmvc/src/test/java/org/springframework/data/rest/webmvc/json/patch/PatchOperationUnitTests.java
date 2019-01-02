/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * General unit tests for {@link PatchOperation} implementations.
 * 
 * @author Oliver Gierke
 */
@RunWith(Parameterized.class)
public class PatchOperationUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	@Parameters
	public static Iterable<? extends PatchOperation> operations() {

		String invalidPath = "/nonExistant";
		String validPath = "/1/description";

		return Arrays.asList( //

				AddOperation.of(invalidPath, null), //
				RemoveOperation.valueAt(invalidPath), //
				ReplaceOperation.valueAt(invalidPath).with(null), //
				TestOperation.whetherValueAt(invalidPath).hasValue(null), //

				CopyOperation.from(invalidPath).to(validPath), //
				CopyOperation.from(validPath).to(invalidPath), //

				MoveOperation.from(invalidPath).to(validPath), //
				MoveOperation.from(validPath).to(invalidPath) //
		);
	}

	public @Parameter(0) PatchOperation operation;

	@Test // DATAREST-1137
	public void invalidPathGetsRejected() {

		Todo todo = new Todo(1L, "A", false);

		exception.expect(PatchException.class);

		operation.perform(todo, Todo.class);
	}
}
