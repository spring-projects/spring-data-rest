/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import org.springframework.data.domain.Pageable;

/**
 * Value object to capture a {@link Pageable} as well it is the default one configured.
 * 
 * @author Oliver Gierke
 */
public class DefaultedPageable {

	private final Pageable pageable;
	private final boolean isDefault;

	/**
	 * Creates a new {@link DefaultedPageable} with the given {@link Pageable} and default flag.
	 * 
	 * @param pageable can be {@literal null}.
	 * @param isDefault
	 */
	public DefaultedPageable(Pageable pageable, boolean isDefault) {

		this.pageable = pageable;
		this.isDefault = isDefault;
	}

	/**
	 * Returns the delegate {@link Pageable}.
	 * 
	 * @return can be {@literal null}.
	 */
	public Pageable getPageable() {
		return pageable;
	}

	/**
	 * Returns whether the contained {@link Pageable} is the default one configured.
	 * 
	 * @return the isDefault
	 */
	public boolean isDefault() {
		return isDefault;
	}
}
