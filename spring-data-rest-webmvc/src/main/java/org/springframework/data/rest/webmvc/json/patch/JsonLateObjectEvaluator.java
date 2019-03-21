/*
 * Copyright 2014-2019 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link LateObjectEvaluator} implementation that assumes values represented as JSON objects.
 *
 * @author Craig Walls
 * @author Oliver Trosien
 * @author Oliver Gierke
 * @author Simon Allegraud
 */
@RequiredArgsConstructor
class JsonLateObjectEvaluator implements LateObjectEvaluator {

	private final @NonNull ObjectMapper mapper;
	private final @NonNull JsonNode valueNode;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator#evaluate(java.lang.Class)
	 */
	@Override
	public Object evaluate(Class<?> type) {

		try {
			return mapper.readValue(valueNode.traverse(mapper.getFactory().getCodec()), type);
		} catch (Exception o_O) {
			throw new PatchException(String.format("Could not read %s into %s!", valueNode, type), o_O);
		}
	}
}
