package org.springframework.data.rest.webmvc.jpa;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
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

		Order order = new Order(person);
		order.add(new LineItem("Java Chip"));
		orders.save(order);
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
