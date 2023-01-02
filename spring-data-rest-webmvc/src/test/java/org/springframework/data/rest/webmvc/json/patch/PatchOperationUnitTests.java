/*
 * Copyright 2017-2023 the original author or authors.
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

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * General unit tests for {@link PatchOperation} implementations.
 *
 * @author Oliver Gierke
 */
class PatchOperationUnitTests {

	@TestFactory // DATAREST-1137
	Stream<DynamicTest> invalidPathGetsRejected() {

		String invalidPath = "/nonExistant";
		String validPath = "/1/description";

		return DynamicTest.stream(Stream.of( //

				AddOperation.of(invalidPath, null), //
				RemoveOperation.valueAt(invalidPath), //
				ReplaceOperation.valueAt(invalidPath).with(null), //
				TestOperation.whetherValueAt(invalidPath).hasValue(null), //

				CopyOperation.from(invalidPath).to(validPath), //
				CopyOperation.from(validPath).to(invalidPath), //

				MoveOperation.from(invalidPath).to(validPath), //
				MoveOperation.from(validPath).to(invalidPath) //

		), it -> it.toString(), it -> {

			Todo todo = new Todo(1L, "A", false);

			assertThatExceptionOfType(PatchException.class) //
					.isThrownBy(() -> it.perform(todo, Todo.class, TestPropertyPathContext.INSTANCE));
		});
	}
}
