/*
 * Copyright 2014-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.tests.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.data.rest.tests.TestMvcClient;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test Spring Data REST in the context of being locked down by Spring Security. Uses MockMvc to simulate HTTP-based
 * interactions. Testing is possible on the repository level, but that doesn't align with the mission of Spring Data
 * REST.
 *
 * @author Greg Turnquist
 * @author Rob Winch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SecurityIntegrationTests.Config.class, SecurityConfiguration.class,
		RepositoryRestMvcConfiguration.class })
class SecurityIntegrationTests extends AbstractWebIntegrationTests {

	@Autowired WebApplicationContext context;

	@Autowired SecuredPersonRepository personRepository;
	@Autowired PreAuthorizedOrderRepository orderRepository;

	@Configuration
	@EnableMapRepositories
	static class Config {}

	@BeforeEach
	@Override
	public void setUp() {

		super.setUp();

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));

		personRepository.deleteAll();
		orderRepository.deleteAll();

		Person frodo = personRepository.save(new Person("Frodo", "Baggins"));
		orderRepository.save(new Order(frodo));

		SecurityContextHolder.clearContext();
	}

	/**
	 * Override the configuration used in {@link AbstractWebIntegrationTests} by plugging in Spring Security's
	 * {@link TestSecurityContextHolderPostProcessor} via apply(springSecurity()).
	 */
	@Override
	protected void setupMockMvc() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).//
				defaultRequest(get("/").//
						accept(TestMvcClient.DEFAULT_MEDIA_TYPE))
				.//
				apply(springSecurity()).//
				build();
		this.mvc = MockMvcTester.create(mockMvc);
	}

	@Test // DATAREST-327
	void deletePersonAccessDeniedForNoCredentials() throws Exception {

		// Getting the collection is not tested here. This is to get the URI that will later be tested for DELETE
		final String people = client.discoverUnique("people").expand().getHref();

		MockHttpServletResponse response = mockMvc.perform(get(people).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.people[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mockMvc.perform(delete(href)).andExpect(status().isUnauthorized());
	}

	@Test // DATAREST-327
	void deletePersonAccessDeniedForUsers() throws Exception {

		MockHttpServletResponse response = mockMvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.people[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mockMvc.perform(delete(href).with(user("user").roles("USER"))).//
				andExpect(status().isForbidden());
	}

	@Test // DATAREST-327
	void deletePersonAccessGrantedForAdmins() throws Exception {

		MockHttpServletResponse response = mockMvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER", "ADMIN"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.people[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mockMvc.perform(delete(href).with(user("user").roles("USER", "ADMIN")))
				.andExpect(status().is2xxSuccessful());
	}

	@Test // DATAREST-327
	void findAllPeopleAccessDeniedForNoCredentials() throws Throwable {

		mockMvc.perform(get(client.discoverUnique("people").expand().getHref())).//
				andExpect(status().isUnauthorized());
	}

	@Test // DATAREST-327
	void findAllPeopleAccessGrantedForUsers() throws Throwable {

		mockMvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andExpect(status().isOk());
	}

	@Test // DATAREST-327
	void findAllPeopleAccessGrantedForAdmins() throws Throwable {

		mockMvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER", "ADMIN"))).//
				andExpect(status().isOk());
	}

	@Test // DATAREST-327
	void deleteOrderAccessDeniedForNoCredentials() throws Exception {

		// Getting the collection is not tested here. This is to get the URI that will later be tested for DELETE
		MockHttpServletResponse response = mockMvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mockMvc.perform(delete(href)).andExpect(status().isUnauthorized());
	}

	@Test // DATAREST-327
	void deleteOrderAccessDeniedForUsers() throws Exception {

		MockHttpServletResponse response = mockMvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		mockMvc.perform(delete(href).with(user("user").roles("USER"))).//
				andExpect(status().isForbidden());
	}

	@Test // DATAREST-327
	void deleteOrderAccessGrantedForAdmins() throws Exception {

		MockHttpServletResponse response = mockMvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mockMvc.perform(delete(href).with(user("user").roles("USER", "ADMIN")))
				.andExpect(status().is2xxSuccessful());
	}

	@Test // DATAREST-327
	void findAllOrdersAccessDeniedForNoCredentials() throws Throwable {

		mockMvc.perform(get(client.discoverUnique("orders").expand().getHref())).//
				andExpect(status().isUnauthorized());
	}

	@Test // DATAREST-327
	void findAllOrdersAccessGrantedForUsers() throws Throwable {

		mockMvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andExpect(status().isOk());
	}

	@Test // DATAREST-327
	void findAllOrdersAccessGrantedForAdmins() throws Throwable {

		mockMvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER", "ADMIN"))).//
				andExpect(status().isOk());
	}

	@Test // #2070
	void rejectsAccessToItemResourceIfNotAuthorized() throws Exception {

		MockHttpServletResponse response = mockMvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		mockMvc.perform(get(href).with(user("user").roles("USER")))
				.andExpect(status().isForbidden());
	}
}
