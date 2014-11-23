/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.cassandra;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.util.Arrays;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for Cassandra repositories
 *
 * @author Greg Turnquist
 */
@ContextConfiguration(classes = CassandraRepoConfig.class)
public class CassandraWebTests extends AbstractCassandraIntegrationTest {

	@Autowired private EmployeeRepository repository;

	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("employees");
	}

	/**
	 * Given that Spring Data Cassandra can leave behind persistent artifacts, need to clean out ALL entities
	 * before launching a given test case.
	 *
	 * @throws ConfigurationException
	 * @throws IOException
	 * @throws TTransportException
	 * @throws InterruptedException
	 */
	@Before
	public void cleanoutDatabase() throws ConfigurationException, IOException, TTransportException, InterruptedException {

		repository.deleteAll();
	}

	@Test
	public void create() throws Exception {

		Link employeeLink = client.discoverUnique("employees");
		ObjectMapper mapper = new ObjectMapper();
		Employee employee = new Employee();
		employee.setId("789");
		employee.setFirstName("Bilbo");
		employee.setLastName("Baggins");
		employee.setTitle("burgler");
		String bilboString = mapper.writeValueAsString(employee);

		MockHttpServletResponse response = postAndGet(employeeLink, bilboString, MediaType.APPLICATION_JSON);

		assertJsonPathEquals("$.firstName", "Bilbo", response);
		assertJsonPathEquals("$.lastName", "Baggins", response);
		assertJsonPathEquals("$.title", "burgler", response);
	}

	/**
	 * After inserting data directly through a Spring Data Cassandra repository, verify Spring Data REST can
	 * fetch the resources through hypermedia.
	 *
	 * @throws Exception
	 */
	@Test
	public void findAllEmployees() throws Exception {

		Employee employee1 = new Employee();
		employee1.setId("123");
		employee1.setFirstName("Frodo");
		employee1.setLastName("Baggins");
		employee1.setTitle("ring bearer");
		repository.save(employee1);

		Employee employee2 = new Employee();
		employee2.setId("789");
		employee2.setFirstName("Samwise");
		employee2.setLastName("Gamgee");
		employee2.setTitle("ring bearer");
		repository.save(employee2);

		Link employeesLink = client.discoverUnique("employees");

		client.follow(employeesLink)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.employees[*].firstName", hasItems("Samwise", "Frodo")))
				.andExpect(jsonPath("$._embedded.employees[*].lastName", hasItems("Gamgee", "Baggins")))
				.andExpect(jsonPath("$._embedded.employees[*].title", hasItems("ring bearer", "ring bearer")));
	}

	/**
	 * Verify some basic creation (POST), updating (PATH) and replacing (PUT) functionality of Spring
	 * Data Cassandra through Spring Data REST.
	 *
	 * @throws Exception
	 */
	@Test
	public void createAnEmployee() throws Exception {

		Employee employee = new Employee();
		employee.setId("123");
		employee.setFirstName("Frodo");
		employee.setLastName("Baggins");
		employee.setTitle("ring bearer");
		ObjectMapper mapper = new ObjectMapper();
		String employeeString = mapper.writeValueAsString(employee);

		Link employeeLink = client.discoverUnique("employees");

		MockHttpServletResponse response = postAndGet(employeeLink, employeeString, MediaType.APPLICATION_JSON);

		Link newlyMintedEmployeeLink = client.assertHasLinkWithRel("self", response);
		Employee newlyMintedEmployee = mapper.readValue(response.getContentAsString(), Employee.class);

		assertThat(newlyMintedEmployee.getFirstName(), equalTo(employee.getFirstName()));
		assertThat(newlyMintedEmployee.getLastName(), equalTo(employee.getLastName()));
		assertThat(newlyMintedEmployee.getTitle(), equalTo(employee.getTitle()));

		MockHttpServletResponse response2 = patchAndGet(
				newlyMintedEmployeeLink, "{\"firstName\": \"Bilbo\"}", MediaType.APPLICATION_JSON);

		Link refurbishedEmployeeLink = client.assertHasLinkWithRel("self", response2);
		Employee refurbishedEmployee = mapper.readValue(response2.getContentAsString(), Employee.class);

		assertThat(refurbishedEmployee.getFirstName(), equalTo("Bilbo"));
		assertThat(refurbishedEmployee.getLastName(), equalTo(employee.getLastName()));
		assertThat(refurbishedEmployee.getTitle(), equalTo(employee.getTitle()));

		MockHttpServletResponse response3 = putAndGet(
				refurbishedEmployeeLink, "{\"lastName\": \"Jr.\"}", MediaType.APPLICATION_JSON);

		Employee lastEmployee = mapper.readValue(response3.getContentAsString(), Employee.class);

		assertThat(lastEmployee.getFirstName(), equalTo("Bilbo"));
		assertThat(lastEmployee.getLastName(), equalTo("Jr."));
		assertThat(lastEmployee.getTitle(), equalTo(employee.getTitle()));
	}

	/**
	 * Verify that creating (POST) and then updating (PATCH) a resource only updates the sub-set of fields.
	 *
	 * @throws Exception
	 */
	@Test
	public void createThenPatch() throws Exception {

		Employee employee = new Employee();
		employee.setId("123");
		employee.setFirstName("Frodo");
		employee.setLastName("Baggins");
		employee.setTitle("ring bearer");
		ObjectMapper mapper = new ObjectMapper();
		String employeeString = mapper.writeValueAsString(employee);

		Link employeeLink = client.discoverUnique("employees");

		MockHttpServletResponse response1 = postAndGet(employeeLink, employeeString, MediaType.APPLICATION_JSON);

		Link newlyMintedEmployeeLink = client.assertHasLinkWithRel("self", response1);
		Employee newlyMintedEmployee = mapper.readValue(response1.getContentAsString(), Employee.class);

		assertThat(newlyMintedEmployee.getFirstName(), equalTo(employee.getFirstName()));
		assertThat(newlyMintedEmployee.getLastName(), equalTo(employee.getLastName()));
		assertThat(newlyMintedEmployee.getTitle(), equalTo(employee.getTitle()));

		MockHttpServletResponse response2 = patchAndGet(
				newlyMintedEmployeeLink, "{\"firstName\": \"Bilbo\"}", MediaType.APPLICATION_JSON);

		Employee refurbishedEmployee = mapper.readValue(response2.getContentAsString(), Employee.class);

		assertThat(refurbishedEmployee.getFirstName(), equalTo("Bilbo"));
		assertThat(refurbishedEmployee.getLastName(), equalTo(employee.getLastName()));
		assertThat(refurbishedEmployee.getTitle(), equalTo(employee.getTitle()));
	}

	/**
	 * Verify that first creating (POST) and then replacing (PUT) a resource with a subset of fields only causes
	 * the subset of fields to be changed inside Cassandra.
	 *
	 * NOTE: Cassandra doesn't handle nulls like traditional databases and Spring Data Cassandra ignores
	 * null fields.
	 *
	 * @throws Exception
	 */
	@Test
	public void createThenPut() throws Exception {

		Employee employee = new Employee();
		employee.setId("123");
		employee.setFirstName("Frodo");
		employee.setLastName("Baggins");
		employee.setTitle("ring bearer");
		ObjectMapper mapper = new ObjectMapper();
		String employeeString = mapper.writeValueAsString(employee);

		Link employeeLink = client.discoverUnique("employees");

		MockHttpServletResponse response1 = postAndGet(employeeLink, employeeString, MediaType.APPLICATION_JSON);

		Link newlyMintedEmployeeLink = client.assertHasLinkWithRel("self", response1);
		Employee newlyMintedEmployee = mapper.readValue(response1.getContentAsString(), Employee.class);

		assertThat(newlyMintedEmployee.getFirstName(), equalTo(employee.getFirstName()));
		assertThat(newlyMintedEmployee.getLastName(), equalTo(employee.getLastName()));
		assertThat(newlyMintedEmployee.getTitle(), equalTo(employee.getTitle()));

		MockHttpServletResponse response2 = putAndGet(
				newlyMintedEmployeeLink, "{\"firstName\": \"Bilbo\"}", MediaType.APPLICATION_JSON);

		Employee refurbishedEmployee = mapper.readValue(response2.getContentAsString(), Employee.class);

		assertThat(refurbishedEmployee.getFirstName(), equalTo("Bilbo"));

		// Spring Data Cassandra doesn't apply null field values, hence these attributes won't change from
		// the original POST.
		assertThat(refurbishedEmployee.getLastName(), equalTo(employee.getLastName()));
		assertThat(refurbishedEmployee.getTitle(), equalTo(employee.getTitle()));
	}

}
