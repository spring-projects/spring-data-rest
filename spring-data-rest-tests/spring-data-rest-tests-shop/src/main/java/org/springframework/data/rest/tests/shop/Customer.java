/*
 * Copyright 2014-2025 the original author or authors.
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
public class Customer {

	private final @Id UUID id = UUID.randomUUID();
	private final String firstname, lastname;
	private final Gender gender;
	private final Address address;

	public Customer(String firstname, String lastname, Gender gender, Address address) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.gender = gender;
		this.address = address;
	}

	public UUID getId() {
		return this.id;
	}

	public String getFirstname() {
		return this.firstname;
	}

	public String getLastname() {
		return this.lastname;
	}

	public Gender getGender() {
		return this.gender;
	}

	public Address getAddress() {
		return this.address;
	}

	static enum Gender {
		MALE, FEMALE;
	}
}
