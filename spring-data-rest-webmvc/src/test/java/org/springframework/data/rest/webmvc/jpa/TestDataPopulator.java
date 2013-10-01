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

	@Autowired
	public TestDataPopulator(PersonRepository people, OrderRepository orders) {
		this.people = people;
		this.orders = orders;
	}

	public void populateRepositories() {

		populatePeople();
		populateOrders();
	}

	private void populateOrders() {

		if (orders.count() != 0) {
			return;
		}

		Person person = people.findAll().iterator().next();

		orders.save(new Order(person));
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
