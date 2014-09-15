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
package org.springframework.data.rest.webmvc.security;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.data.rest.webmvc.TestMvcClient;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

/**
 * Test Spring Data REST in the context of being locked down by Spring Security. Uses MockMvc to simulate HTTP-based
 * interactions. Testing is possible on the repository level, but that doesn't align with the mission
 * of Spring Data REST.
 *
 * @author Greg Turnquist
 * @author Rob Winch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SecureJpaConfiguration.class, SecurityConfiguration.class})
@Transactional
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class SecurityIntegrationTests extends AbstractWebIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired MethodSecurityInterceptor methodSecurityInterceptor;

	@Autowired SecuredPersonRepository personRepository;
	@Autowired PreAuthorizedOrderRepository orderRepository;

	@Before
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
		this.mvc = MockMvcBuilders.webAppContextSetup(context).//
				defaultRequest(get("/").//
				accept(TestMvcClient.DEFAULT_MEDIA_TYPE)).//
				apply(springSecurity()).//
				build();
	}

	//=================================================================

	@Test
	public void deletePersonAccessDeniedForNoCredentials() throws Exception {

		// Getting the collection is not tested here. This is to get the URI that will later be tested for DELETE
		final String people = client.discoverUnique("people").expand().getHref();

		MockHttpServletResponse response = mvc.perform(get(people).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.people[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		MockHttpServletResponse deleteResponse = mvc.perform(delete(href)).//
				andExpect(status().isUnauthorized()).//
				andReturn().getResponse();
		assertThat(deleteResponse.getErrorMessage(), is("Full authentication is required to access this resource"));
	}

	@Test
	public void deletePersonAccessDeniedForUsers() throws Exception {

		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.people[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		MockHttpServletResponse deleteResponse = mvc.perform(delete(href).//
				with(user("user").roles("USER"))).//
				andExpect(status().isForbidden()).//
				andReturn().getResponse();
		assertThat(deleteResponse.getErrorMessage(), is("Access is denied"));
	}

	@Test
	public void deletePersonAccessGrantedForAdmins() throws Exception {

		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER", "ADMIN"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.people[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mvc.perform(delete(href).with(user("user").roles("USER", "ADMIN")))
			.andExpect(status().is(HttpStatus.NO_CONTENT.value()));
	}

	//=================================================================

	@Test
	public void findAllPeopleAccessDeniedForNoCredentials() throws Throwable {

		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("people").expand().getHref())).//
				andExpect(status().isUnauthorized()).//
				andReturn().getResponse();
		assertThat(response.getErrorMessage(), is("Full authentication is required to access this resource"));
	}

	@Test
	public void findAllPeopleAccessGrantedForUsers() throws Throwable {

		mvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andExpect(status().isOk());
	}

	@Test
	public void findAllPeopleAccessGrantedForAdmins() throws Throwable {

		mvc.perform(get(client.discoverUnique("people").expand().getHref()).//
				with(user("user").roles("USER", "ADMIN"))).//
				andExpect(status().isOk());
	}

	//=================================================================


	@Test
	public void deleteOrderAccessDeniedForNoCredentials() throws Exception {

		// Getting the collection is not tested here. This is to get the URI that will later be tested for DELETE
		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		MockHttpServletResponse deleteResponse = mvc.perform(delete(href)).//
				andExpect(status().isUnauthorized()).//
				andReturn().getResponse();
		assertThat(deleteResponse.getErrorMessage(), is("Full authentication is required to access this resource"));
	}

	@Test
	public void deleteOrderAccessDeniedForUsers() throws Exception {

		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		MockHttpServletResponse deleteResponse = mvc.perform(delete(href).with(user("user").roles("USER"))).//
				andExpect(status().isForbidden()).//
				andReturn().getResponse();
		assertThat(deleteResponse.getErrorMessage(), is("Access is denied"));
	}

	@Test
	public void deleteOrderAccessGrantedForAdmins() throws Exception {

		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andReturn().getResponse();
		String href = assertHasJsonPathValue("$._embedded.orders[0]._links.self.href", response);

		// Clear any side effects of logging in to get the URI from security.
		SecurityContextHolder.clearContext();

		mvc.perform(delete(href).with(user("user").roles("USER", "ADMIN")))
				.andExpect(status().is(HttpStatus.NO_CONTENT.value()));
	}

	//=================================================================

	@Test
	public void findAllOrdersAccessDeniedForNoCredentials() throws Throwable {

		MockHttpServletResponse response = mvc.perform(get(client.discoverUnique("orders").expand().getHref())).//
			andExpect(status().isUnauthorized()).//
			andReturn().getResponse();
		assertThat(response.getErrorMessage(), is("Full authentication is required to access this resource"));
	}

	@Test
	public void findAllOrdersAccessGrantedForUsers() throws Throwable {

		mvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER"))).//
				andExpect(status().isOk());
	}

	@Test
	public void findAllOrdersAccessGrantedForAdmins() throws Throwable {

		mvc.perform(get(client.discoverUnique("orders").expand().getHref()).//
				with(user("user").roles("USER", "ADMIN"))).//
				andExpect(status().isOk());
	}

}
