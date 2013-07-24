package org.springframework.data.rest.webmvc.jpa;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
 */
@Component
public class PersonLoader {

	private final PersonRepository people;

	@Autowired
	public PersonLoader(PersonRepository people) {
		this.people = people;
	}

	public void populateRepository() {

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
