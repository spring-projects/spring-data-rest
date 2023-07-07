/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.rest.core.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Reference;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * An entity that represents a person.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class Person {

	private final @Id UUID id;
	private final String firstName, lastName;

	public Person(String firstName, String lastName) {
		this(UUID.randomUUID(), firstName, lastName);
	}

	private @Reference List<Person> siblings = new ArrayList<Person>();
	private @RestResource(path = "father-mapped") @Reference Person father;
	private Date created = Calendar.getInstance().getTime();

	@PersistenceCreator
	private Person(UUID id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Person addSibling(Person p) {
		siblings.add(p);
		return this;
	}

	public UUID getId() {
		return this.id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public List<Person> getSiblings() {
		return this.siblings;
	}

	public Person getFather() {
		return this.father;
	}

	public Date getCreated() {
		return this.created;
	}

	public void setSiblings(List<Person> siblings) {
		this.siblings = siblings;
	}

	public void setFather(Person father) {
		this.father = father;
	}

	public void setCreated(Date created) {
		this.created = created;
	}
}
