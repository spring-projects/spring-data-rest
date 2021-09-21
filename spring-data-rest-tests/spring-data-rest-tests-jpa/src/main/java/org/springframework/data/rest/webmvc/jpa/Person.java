/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.data.rest.core.annotation.Description;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An entity that represents a person.
 *
 * @author Jon Brisbin
 */
@Entity
@JsonIgnoreProperties({ "height", "weight" })
public class Person {

	@Id @GeneratedValue private Long id;

	@Description("A person's first name") //
	private String firstName;

	@Description("A person's last name") //
	private String lastName;

	@Description("A person's siblings") //
	@ManyToMany //
	private List<Person> siblings = new ArrayList<Person>();

	@ManyToOne //
	private Person father;

	@Description("Timestamp this person object was created") //
	private Date created;

	@JsonIgnore //
	private int age;

	private int height, weight;
	private Gender gender;

	public Person() {}

	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

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

	public List<Person> getSiblings() {
		return siblings;
	}

	public void setSiblings(List<Person> siblings) {
		this.siblings = siblings;
	}

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

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public static enum Gender {
		MALE, FEMALE, UNDEFINED;
	}
}
