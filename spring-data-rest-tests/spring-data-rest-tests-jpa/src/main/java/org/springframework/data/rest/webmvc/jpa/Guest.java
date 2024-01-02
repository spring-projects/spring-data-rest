/*
 * Copyright 2015-2024 the original author or authors.
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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Leigh
 */
@Entity
public class Guest {

	@Id @GeneratedValue //
	private Long id;

	@OneToOne(cascade = CascadeType.ALL) //
	private Room room;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) //
	private List<Meal> meals = new ArrayList<Meal>();

	public void addMeal(Meal meal) {
		this.meals.add(meal);
	}

	public Long getId() {
		return this.id;
	}

	public Room getRoom() {
		return this.room;
	}

	public List<Meal> getMeals() {
		return this.meals;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public void setMeals(List<Meal> meals) {
		this.meals = meals;
	}
}
