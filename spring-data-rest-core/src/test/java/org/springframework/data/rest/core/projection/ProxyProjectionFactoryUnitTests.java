/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.core.projection;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.aop.TargetClassAware;
import org.springframework.beans.factory.annotation.Value;

/**
 * Unit tests for {@link ProxyProjectionFactory}.
 * 
 * @author Oliver Gierke
 */
public class ProxyProjectionFactoryUnitTests {

	ProjectionFactory factory = new ProxyProjectionFactory(null);

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void createsProjectingProxy() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		CustomerExcerpt excerpt = factory.createProjection(customer, CustomerExcerpt.class);

		assertThat(excerpt, is(instanceOf(TargetClassAware.class)));
		assertThat(excerpt.getFirstname(), is("Dave"));
		assertThat(excerpt.getAddress().getZipCode(), is("ZIP"));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void proxyExposesTargetClassAware() {
		assertThat(factory.createProjection(new Object(), CustomerExcerpt.class), is(instanceOf(TargetClassAware.class)));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonInterfacesAsProjectionTarget() {
		factory.createProjection(new Object(), Object.class);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void exposesSpelInvokingMethod() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		CustomerExcerpt excerpt = factory.createProjection(customer, CustomerExcerpt.class);
		assertThat(excerpt.getFullName(), is("Dave Matthews"));
	}

	static class Customer {

		public String firstname, lastname;
		public Address address;
	}

	static class Address {

		public String zipCode, city;
	}

	interface CustomerExcerpt {

		String getFirstname();

		AddressExcerpt getAddress();

		@Value("#{target.firstname + ' ' + target.lastname}")
		String getFullName();
	}

	interface AddressExcerpt {

		String getZipCode();
	}
}
