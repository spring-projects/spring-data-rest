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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test Spring Data REST in the context of being locked down by Spring Security
 *
 * @author Greg Turnquist
 * @author Rob Winch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SecureJpaConfiguration.class, SecurityConfiguration.class})
@Transactional
public class SecurityIntegrationTests extends AbstractWebIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired MethodSecurityInterceptor methodSecurityInterceptor;

	@Autowired SecuredPersonRepository personRepository;
	@Autowired PreAuthorizedOrderRepository orderRepository;

	SecurityTestUtils securityTestUtils;

	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("people", "orders");
	}

	@Override
	public void setUp() {

		super.setUp();
		securityTestUtils = new SecurityTestUtils(methodSecurityInterceptor);
	}

	@Before
	public void clearContext() {
		SecurityContextHolder.clearContext();
	}

	//=================================================================

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testNoCredentialsForPeopleDeleteAll() {
		personRepository.deleteAll();
	}

	@Test
	public void deleteAllPeopleAccessDeniedForUsers() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(securityTestUtils.hasAccess(personRepository, "deleteAll"), is(false));
	}

	@Test
	public void deleteAllPeopleAccessGrantedForAdmins() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));

		assertThat(securityTestUtils.hasAccess(personRepository, "deleteAll"), is(true));
	}

	//=================================================================

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testNoCredentialsForPeopleFindAll() {
		personRepository.findAll();
	}

	@Test
	public void findAllPeopleAccessGrantedForUsers() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(securityTestUtils.hasAccess(personRepository, "findAll"), is(true));
	}

	@Test
	public void findAllPeopleAccessGrantedForAdmins() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));

		assertThat(securityTestUtils.hasAccess(personRepository, "findAll"), is(true));
	}

	//=================================================================

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testNoCredentialsForOrdersDeleteAll() {
		orderRepository.deleteAll();
	}

	@Test
	public void deleteAllOrdersAccessDeniedForUsers() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(securityTestUtils.hasAccess(orderRepository, "deleteAll"), is(false));
	}

	@Test
	public void deleteAllOrdersAccessGrantedForAdmins() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));

		assertThat(securityTestUtils.hasAccess(orderRepository, "deleteAll"), is(true));
	}

	//=================================================================

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void testNoCredentialsForOrdersFindAll() {
		orderRepository.findAll();
	}

	@Test
	public void findAllOrdersAccessGrantedForUsers() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(securityTestUtils.hasAccess(orderRepository, "findAll"), is(true));
	}

	@Test
	public void findAllOrdersAccessGrantedForAdmins() throws Throwable {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "user",
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));

		assertThat(securityTestUtils.hasAccess(orderRepository, "findAll"), is(true));
	}

}
