/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.rest.core.domain.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;

/**
 * An entity that represents a person.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@Entity
public class Person {

	@Id @GeneratedValue private Long id;
	private String firstName;
	private String lastName;
	@OneToMany private List<Person> siblings = Collections.emptyList();
	private Date created;

	public Person() {}

	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Person addSibling(Person p) {
		if (siblings == Collections.EMPTY_LIST) {
			siblings = new ArrayList<Person>();
		}
		siblings.add(p);
		return this;
	}

	public List<Person> getSiblings() {
		return siblings;
	}

	public void setSiblings(List<Person> siblings) {
		this.siblings = siblings;
	}

	public Date getCreated() {
		return created;
	}

	@PrePersist
	private void prePersist() {
		this.created = Calendar.getInstance().getTime();
	}

}
