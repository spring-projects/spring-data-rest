/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.security;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.rest.core.annotation.Description;

/**
 * An entity that represents a person.
 * 
 * @author Jon Brisbin
 */
@Entity
@JsonIgnoreProperties({ "height", "weight" })
public class Person {

	private Long id;
	@Description("A person's first name") private String firstName;
	@Description("A person's last name") private String lastName;
	@Description("A person's siblings") private List<Person> siblings = Collections.emptyList();
	private Person father;
	@Description("Timestamp this person object was created") private Date created;
	private int age, height, weight;

	public Person() {}

	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	@NotNull
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

	@ManyToMany
	public List<Person> getSiblings() {
		return siblings;
	}

	public void setSiblings(List<Person> siblings) {
		this.siblings = siblings;
	}

	@ManyToOne
	public Person getFather() {
		return father;
	}

	public void setFather(Person father) {
		this.father = father;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {}

	@PrePersist
	private void prePersist() {
		this.created = Calendar.getInstance().getTime();
	}

	@JsonIgnore
	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}
