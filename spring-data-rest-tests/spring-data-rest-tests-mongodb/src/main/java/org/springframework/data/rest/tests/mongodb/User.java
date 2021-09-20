/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.rest.tests.mongodb;

import lombok.Value;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Oliver Gierke
 */
@Document
public class User {

	public static enum Gender {
		MALE, FEMALE;
	}

	public BigInteger id;
	public String firstname, lastname;
	public Address address;
	public Set<Address> shippingAddresses;
	public List<String> nicknames;
	public Gender gender;
	public @ReadOnlyProperty EmailAddress email;
	public LocalDateTime java8DateTime;
	public TypeWithPattern pattern;
	public @DBRef(lazy = true) List<User> colleagues;
	public @DBRef(lazy = true) User manager;
	public @DBRef(lazy = true) Map<String, User> map;
	public Map<String, Nested> colleaguesMap = new HashMap<String, Nested>();

	public static class EmailAddress {

		private final String value;

		/**
		 * @param value
		 */
		public EmailAddress(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return value;
		}
	}

	public static class TypeWithPattern {}

	@Value
	public static class Nested {
		public @DBRef(lazy = true) User user;
		public String foo = "foo";
	}
}
