/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.Optional;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

public class TestPropertyPathContext implements BindContext {

	public static final BindContext INSTANCE = new TestPropertyPathContext();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.BindContext#getReadableProperty(java.lang.String, java.lang.Class)
	 */
	@Override
	public Optional<String> getReadableProperty(String segment, Class<?> type) {
		return Optional.of(segment);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.BindContext#getWritableProperty(java.lang.String, java.lang.Class)
	 */
	@Override
	public Optional<String> getWritableProperty(String segment, Class<?> type) {
		return Optional.of(segment);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.json.patch.BindContext#getEvaluationContext()
	 */
	@Override
	public EvaluationContext getEvaluationContext() {
		return SimpleEvaluationContext.forReadWriteDataBinding().build();
	}
}
