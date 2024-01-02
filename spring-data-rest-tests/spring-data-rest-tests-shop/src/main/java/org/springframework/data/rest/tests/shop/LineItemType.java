/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.data.rest.tests.shop;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.util.ObjectUtils;

/**
 * @author Oliver Gierke
 */
public class LineItemType {

	private final @Id UUID id = UUID.randomUUID();
	private final String name;

	public LineItemType(String name) {
		this.name = name;
	}

	public UUID getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		LineItemType that = (LineItemType) o;

		return ObjectUtils.nullSafeEquals(id, that.id);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(id);
	}
}
