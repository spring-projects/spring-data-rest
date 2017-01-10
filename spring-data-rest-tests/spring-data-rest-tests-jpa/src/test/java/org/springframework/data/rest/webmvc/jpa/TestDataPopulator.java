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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.jpa.Book.Offer;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class TestDataPopulator {

	@Autowired PersonRepository people;
	@Autowired OrderRepository orders;
	@Autowired AuthorRepository authors;
	@Autowired BookRepository books;

	public void populateRepositories() {

		books.deleteAll();
		authors.deleteAll();
		orders.deleteAll();
		people.deleteAll();

		populatePeople();
		populateOrders();
		populateAuthorsAndBooks();
	}

	private void populateAuthorsAndBooks() {

		Author ollie = new Author("Ollie");
		Author mark = new Author("Mark");
		Author michael = new Author("Michael");
		Author david = new Author("David");
		Author john = new Author("John");
		Author thomas = new Author("Thomas");

		Iterable<Author> authors = this.authors.save(Arrays.asList(ollie, mark, michael, david, john, thomas));

		books.save(new Book("1449323952", "Spring Data", 1000, authors, new Offer(21.21, "EUR")));
		books.save(new Book("1449323953", "Spring Data (Second Edition)", 2000, authors, new Offer(30.99, "EUR")));
	}

	private void populateOrders() {

		Person person = people.findAll().iterator().next();

		Order order = new Order(person);
		order.add(new LineItem("Java Chip"));
		orders.save(order);
	}

	private void populatePeople() {

		Person billyBob = people.save(new Person("Billy Bob", "Thornton"));

		Person john = new Person("John", "Doe");
		Person jane = new Person("Jane", "Doe");
		john.addSibling(jane);
		john.setFather(billyBob);
		jane.addSibling(john);
		jane.setFather(billyBob);

		people.save(Arrays.asList(john, jane));
	}
}
