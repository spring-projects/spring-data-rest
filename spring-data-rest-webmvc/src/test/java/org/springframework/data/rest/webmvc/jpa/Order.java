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
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Oliver Gierke
 */
@Entity
@Table(name = "ORDERS")
public class Order {

	@Id 
	@GeneratedValue(generator = "TransactionalIDGenerator")
	@GenericGenerator(name = "TransactionalIDGenerator",
	        strategy = "org.springframework.data.rest.webmvc.jpa.TransactionalIDGenerator")		
	private Long id;
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)//
	private Person creator;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)//
	private List<LineItem> lineItems = new ArrayList<LineItem>();

	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)//
	@JsonProperty("GSTFreeLineItems")
	private List<TaxFreeItem> taxFreeLineItems = new ArrayList<TaxFreeItem>();
	
	public List<TaxFreeItem> getTaxFreeLineItems() {
		return taxFreeLineItems;
	}

	public void addTaxFree(TaxFreeItem item) {
		this.taxFreeLineItems.add(item);
	}
	
	public Order(Person creator) {
		this.creator = creator;
	}

	protected Order() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	private String orderName;

	public String getOrderName() {
		return orderName;
	}

	public void setOrderName(String orderName) {
		this.orderName = orderName;
	}

	@Transient
	@JsonIgnore
	public double getTax() {
		return 15.21;
	}

	@JsonProperty("internalCode")
	private String IOC; // (internal order code)

	@JsonProperty("internalCode")
	public String getIOC() {
		return IOC;
	}

	@JsonProperty("internalCode")
	public void setIOC(String iOC) {
		IOC = iOC;
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
}
