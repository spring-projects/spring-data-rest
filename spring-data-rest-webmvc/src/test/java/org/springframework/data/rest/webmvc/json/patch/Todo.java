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

package org.springframework.data.rest.webmvc.json.patch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.ObjectUtils;

/**
 * @author Roy Clarkson
 * @author Craig Walls
 * @author Mathias Düsterhöft
 * @author Oliver Gierke
 */
class Todo {

	private Long id;
	private String description;
	private boolean complete;
	private TodoType type = new TodoType();
	private List<String> items = new ArrayList<String>();
	private List<String> uninitialized;
	private BigInteger amount;

	public Todo(Long id, String description, boolean complete) {

		this.id = id;
		this.description = description;
		this.complete = complete;
	}

	public Todo() {}

	public Long getId() {
		return this.id;
	}

	public String getDescription() {
		return this.description;
	}

	public boolean isComplete() {
		return this.complete;
	}

	public TodoType getType() {
		return this.type;
	}

	public List<String> getItems() {
		return this.items;
	}

	public List<String> getUninitialized() {
		return this.uninitialized;
	}

	public BigInteger getAmount() {
		return this.amount;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public void setType(TodoType type) {
		this.type = type;
	}

	public void setItems(List<String> items) {
		this.items = items;
	}

	public void setUninitialized(List<String> uninitialized) {
		this.uninitialized = uninitialized;
	}

	public void setAmount(BigInteger amount) {
		this.amount = amount;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Todo todo = (Todo) o;

		if (complete != todo.complete)
			return false;
		if (!ObjectUtils.nullSafeEquals(id, todo.id)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(description, todo.description)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(type, todo.type)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(items, todo.items)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(uninitialized, todo.uninitialized)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(amount, todo.amount);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(id);
		result = 31 * result + ObjectUtils.nullSafeHashCode(description);
		result = 31 * result + (complete ? 1 : 0);
		result = 31 * result + ObjectUtils.nullSafeHashCode(type);
		result = 31 * result + ObjectUtils.nullSafeHashCode(items);
		result = 31 * result + ObjectUtils.nullSafeHashCode(uninitialized);
		result = 31 * result + ObjectUtils.nullSafeHashCode(amount);
		return result;
	}
}
