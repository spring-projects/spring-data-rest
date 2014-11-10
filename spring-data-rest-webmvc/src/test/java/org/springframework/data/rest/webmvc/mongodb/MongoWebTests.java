/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.mongodb;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.CommonWebTests;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for MongoDB repositories.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@ContextConfiguration(classes = MongoDbRepositoryConfig.class)
public class MongoWebTests extends CommonWebTests {

	@Autowired ProfileRepository repository;
	@Autowired UserRepository userRepository;

	@Before
	public void populateProfiles() {

		Profile twitter = new Profile();
		twitter.setPerson(1L);
		twitter.setType("Twitter");

		Profile linkedIn = new Profile();
		linkedIn.setPerson(1L);
		linkedIn.setType("LinkedIn");

		repository.save(Arrays.asList(twitter, linkedIn));

		Address address = new Address();
		address.street = "Foo";
		address.zipCode = "Bar";

		User user = new User();
		user.firstname = "Oliver";
		user.lastname = "Gierke";
		user.address = address;

		userRepository.save(user);
	}

	@After
	public void cleanUp() {
		repository.deleteAll();
		userRepository.deleteAll();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("profiles", "users");
	}

	@Test
	public void foo() throws Exception {

		Link profileLink = client.discoverUnique("profiles");
		client.follow(profileLink).//
				andExpect(jsonPath("$._embedded.profiles").value(hasSize(2)));
	}

	@Test
	public void rendersEmbeddedDocuments() throws Exception {

		Link usersLink = client.discoverUnique("users");
		Link userLink = assertHasContentLinkWithRel("self", client.request(usersLink));
		client.follow(userLink).//
				andExpect(jsonPath("$.address.zipCode").value(is(notNullValue())));
	}

	/**
	 * @see DATAREST-247
	 */
	@Test
	public void executeQueryMethodWithPrimitiveReturnType() throws Exception {

		Link profiles = client.discoverUnique("profiles");
		Link profileSearches = client.discoverUnique(profiles, "search");
		Link countByTypeLink = client.discoverUnique(profileSearches, "countByType");

		assertThat(countByTypeLink.isTemplated(), is(true));
		assertThat(countByTypeLink.getVariableNames(), hasItem("type"));

		MockHttpServletResponse response = client.request(countByTypeLink.expand("Twitter"));
		assertThat(response.getContentAsString(), is("1"));
	}

	@Test
	public void testname() throws Exception {

		Link usersLink = client.discoverUnique("users");
		Link userLink = assertHasContentLinkWithRel("self", client.request(usersLink));

		MockHttpServletResponse response = patchAndGet(userLink,
				"{\"lastname\" : null, \"address\" : { \"zipCode\" : \"ZIP\"}}", MediaType.APPLICATION_JSON);

		assertThat(JsonPath.read(response.getContentAsString(), "$.lastname"), is(nullValue()));
		assertThat(JsonPath.read(response.getContentAsString(), "$.address.zipCode"), is((Object) "ZIP"));
	}

	@Test
	public void testname2() throws Exception {

		Link usersLink = client.discoverUnique("users");
		Link userLink = assertHasContentLinkWithRel("self", client.request(usersLink));

		MockHttpServletResponse response = patchAndGet(userLink,
				"[{ \"op\": \"replace\", \"path\": \"/address/zipCode\", \"value\": \"ZIP\" },"
				// + "{ \"op\": \"replace\", \"path\": \"/lastname\", \"value\": null }]", //
						+ "{ \"op\": \"remove\", \"path\": \"/lastname\" }]", //
				RestMediaTypes.JSON_PATCH_JSON);

		assertThat(JsonPath.read(response.getContentAsString(), "$.lastname"), is(nullValue()));
		assertThat(JsonPath.read(response.getContentAsString(), "$.address.zipCode"), is((Object) "ZIP"));
	}
}
