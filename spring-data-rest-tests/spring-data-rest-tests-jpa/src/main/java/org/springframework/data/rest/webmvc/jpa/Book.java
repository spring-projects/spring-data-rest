/*
 * Copyright 2013-2017 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Entity
public class Book {

	public @Id @GeneratedValue Long id;
	public String isbn, title;

	@JsonProperty("sales")
	public long soldUnits;

	@ManyToMany(cascade = { CascadeType.MERGE }) //
	@RestResource(path = "creators") //
	public Set<Author> authors;

	public Offer offer;

	protected Book() {}

	public Book(String isbn, String title, long soldUnits, Iterable<Author> authors, Offer offer) {

		this.isbn = isbn;
		this.title = title;
		this.soldUnits = soldUnits;

		this.authors = new HashSet<Author>();

		for (Author author : authors) {
			author.books.add(this);
			this.authors.add(author);
		}

		this.offer = offer;
	}

	@Getter
	@Embeddable
	@AllArgsConstructor
	@NoArgsConstructor
	static class Offer {

		double price;
		String currency;
	}
}
