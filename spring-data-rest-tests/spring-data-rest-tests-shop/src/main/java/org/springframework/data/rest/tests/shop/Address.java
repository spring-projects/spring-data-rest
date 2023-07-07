/*
 * Copyright 2014-2023 the original author or authors.
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

/**
 * @author Oliver Gierke
 */
public class Address {

	private @Id UUID id = UUID.randomUUID();
	private final String street, zipCode, city, state;

	public Address(String street, String zipCode, String city, String state) {
		this.street = street;
		this.zipCode = zipCode;
		this.city = city;
		this.state = state;
	}

	public String toString() {
		return String.format("%s, %s %s, %s", street, zipCode, city, state);
	}

	public UUID getId() {
		return this.id;
	}

	public String getStreet() {
		return this.street;
	}

	public String getZipCode() {
		return this.zipCode;
	}

	public String getCity() {
		return this.city;
	}

	public String getState() {
		return this.state;
	}

	public void setId(UUID id) {
		this.id = id;
	}
}
