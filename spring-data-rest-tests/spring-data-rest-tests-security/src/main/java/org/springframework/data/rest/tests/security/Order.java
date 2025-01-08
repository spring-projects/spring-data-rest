/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.rest.tests.security;

import java.util.UUID;

import org.springframework.data.annotation.Id;

/**
 * @author Oliver Gierke
 */
public final class Order {

	@Id private final UUID id = UUID.randomUUID();
	private final Person customer;

	public Order(Person customer) {
		this.customer = customer;
	}

	public UUID getId() {
		return this.id;
	}

	public Person getCustomer() {
		return this.customer;
	}

}
