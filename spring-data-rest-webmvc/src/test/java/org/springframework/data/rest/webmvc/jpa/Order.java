/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Oliver Gierke
 */
@Entity
@Table(name = "ORDERS")
public class Order {

	@Id @GeneratedValue//
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY)//
	private Person creator;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)//
	private List<LineItem> lineItems = new ArrayList<LineItem>();
	private Type type = Type.TAKE_AWAY;

	public Order(Person creator) {
		this.creator = creator;
	}

	protected Order() {

	}

	public Long getId() {
		return id;
	}

	public Person getCreator() {
		return creator;
	}

	/**
	 * @return the lineItems
	 */
	public List<LineItem> getLineItems() {
		return lineItems;
	}

	public void add(LineItem item) {
		this.lineItems.add(item);
	}

	public BigDecimal getPrice() {
		return new BigDecimal(2.50);
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}
}
