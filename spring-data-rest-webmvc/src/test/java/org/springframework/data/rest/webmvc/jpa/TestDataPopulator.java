/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
 * @author Nick Weedon
 */
@Component
public class TestDataPopulator {

	private final PersonRepository people;
	private final OrderRepository orders;
	private final AuthorRepository authorRepository;
	private final BookRepository books;

	@Autowired
	public TestDataPopulator(PersonRepository people, OrderRepository orders, AuthorRepository authors,
			BookRepository books) {

		this.people = people;
		this.orders = orders;
		this.authorRepository = authors;
		this.books = books;
	}

	public void populateRepositories() {

		populatePeople();
		populateOrders();
		populateAuthorsAndBooks();
	}

	private void populateAuthorsAndBooks() {

		if (authorRepository.count() != 0 || books.count() != 0) {
			return;
		}

		Author ollie = new Author("Ollie");
		Author mark = new Author("Mark");
		Author michael = new Author("Michael");
		Author david = new Author("David");
		Author john = new Author("John");
		Author thomas = new Author("Thomas");

		Iterable<Author> authors = authorRepository.save(Arrays.asList(ollie, mark, michael, david, john, thomas));

		books.save(new Book("1449323952", "Spring Data", authors));
		books.save(new Book("1449323953", "Spring Data (SecondEdition)", authors));
	}

	private void populateOrders() {

		if (orders.count() != 0) {
			return;
		}

		Person person = people.findAll().iterator().next();

		Deque<Order> orderList = new ArrayDeque<Order>();
		
		orderList.add(new Order(person));
		orderList.getLast().setOrderName("Billy bob's chipies");
		orderList.getLast().setIOC("IOC-526");
		orderList.getLast().add(new LineItem("Java Chip"));

		orderList.add(new Order(person));
		orderList.getLast().setOrderName("Jane's makeup obsession");
		orderList.getLast().setIOC("IOC-257");
		orderList.getLast().add(new LineItem("Black super tart eyeliner"));
		
		orders.save(orderList);
	}

	private void populatePeople() {

		if (people.count() != 0) {
			return;
		}

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
