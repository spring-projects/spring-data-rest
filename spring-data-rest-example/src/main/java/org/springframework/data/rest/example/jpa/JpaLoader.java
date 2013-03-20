package org.springframework.data.rest.example.jpa;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jon Brisbin
 */
public class JpaLoader {

	private final static Logger LOG = LoggerFactory.getLogger(JpaLoader.class);
	@Autowired
	PersonRepository people;

	@Transactional
	public void loadData() {
		Person billyBob = people.save(new Person("Billy Bob", "Thornton"));
		LOG.info("Saved {}", billyBob);
		Person john = people.save(new Person("John", "Doe"));
		LOG.info("Saved {}", john);
		Person jane = people.save(new Person("Jane", "Doe"));
		LOG.info("Saved {}", jane);
		john.setSiblings(Arrays.asList(jane));
		john.setFather(billyBob);
		jane.setSiblings(Arrays.asList(john));
		jane.setFather(billyBob);

		people.save(Arrays.asList(john, jane));

		LOG.info("Person count: {}", people.count());
	}

}
