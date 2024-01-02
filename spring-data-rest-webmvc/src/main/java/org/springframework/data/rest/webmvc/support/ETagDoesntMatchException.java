/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import org.springframework.util.Assert;

/**
 * An exception being thrown in case the {@link ETag} calculated for a particular object does not match an expected one.
 *
 * @author Oliver Gierke
 * @see ETag#verify(org.springframework.data.mapping.PersistentEntity, Object)
 */
public class ETagDoesntMatchException extends RuntimeException {

	private static final long serialVersionUID = 415835592506644699L;

	private final ETag expected;
	private final Object bean;

	/**
	 * Creates a new {@link ETagDoesntMatchException} for the given bean as well as the {@link ETag} it was expected to
	 * match.
	 *
	 * @param bean must not be {@literal null}.
	 * @param expected must not be {@literal null}.
	 */
	public ETagDoesntMatchException(Object bean, ETag expected) {

		Assert.notNull(bean, "Target bean must not be null");
		Assert.notNull(expected, "Expected ETag must not be null");

		this.expected = expected;
		this.bean = bean;
	}

	/**
	 * Returns the bean not matching the {@link ETag}.
	 *
	 * @return the bean
	 */
	public Object getBean() {
		return bean;
	}

	/**
	 * Returns the {@link ETag} the bean was expected to match.
	 *
	 * @return the expected
	 */
	public ETag getExpectedETag() {
		return expected;
	}
}
