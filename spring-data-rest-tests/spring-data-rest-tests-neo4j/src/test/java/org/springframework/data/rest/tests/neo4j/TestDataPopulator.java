/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.data.rest.tests.neo4j;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mark Angrish
 */
public class TestDataPopulator {

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	CountryRepository countryRepository;

	public void populateRepositories() {

		customerRepository.deleteAll();
		countryRepository.deleteAll();

		populateCountriesCustomersAndAddresses();
	}


	public void populateCountriesCustomersAndAddresses() {

		final Country ae = new Country("AE", "United Arab Emirates");
		final Country au = new Country("AU", "Australia");
		final Country de = new Country("DE", "Germany");
		final Country gb = new Country("GB", "Great Britain");
		final Country in = new Country("IN", "India");
		final Country ph = new Country("PH", "Philippines");

		final Customer oliver = new Customer("Oliver", "Gierke", "oliver@gierke.com");
		oliver.add(new Address("Pivotal Street", "Dresden", de));

		final Customer mark = new Customer("Mark", "Angrish", "mark@angrish.com");
		mark.add(new Address("New Baby Street", "Melbourne", au));
		mark.add(new Address("Single Life Street", "London", gb));

		final Customer luanne = new Customer("Luanne", "Misquitta", "luanne@misquitta.com");
		luanne.add(new Address("Exciting Times Street", "Abu Dhabi", ae));
		luanne.add(new Address("What Happened to my City Street", "Goa", in));

		final Customer vince = new Customer("Vince", "Bickers", "vince@bickers.com");
		vince.add(new Address("Ol' Blighty Street", "London", gb));

		final Customer jasper = new Customer("Jasper", "Blues", "jasper@blues.com");
		jasper.add(new Address("Tropical Paradise Street", "Manilla", ph));

		final Customer michael = new Customer("Michael", "Hunger", "michael@hunger.com");
		michael.add(new Address("Neo Street", "Dresden", gb));

		final Customer michal = new Customer("Michal", "Bachman", "michal@bachman.com");
		michal.add(new Address("Bachmainia Street", "Abu Dhabi", ae));
		michal.add(new Address("Graphaware Street", "London", gb));

		// save through reachability.
		customerRepository.save(Arrays.asList(oliver, mark, luanne, vince, jasper, michael, michal));
	}
}
