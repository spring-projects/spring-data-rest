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
package org.springframework.data.rest.webmvc.mongodb;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	ObjectMapper mapper = new ObjectMapper();

	@Before
	public void populateProfiles() {

		mapper.setSerializationInclusion(Include.NON_NULL);

		Profile twitter = new Profile();
		twitter.setPerson(1L);
		twitter.setType("Twitter");

		Profile linkedIn = new Profile();
		linkedIn.setPerson(1L);
		linkedIn.setType("LinkedIn");

		repository.save(Arrays.asList(twitter, linkedIn));

		Address address = new Address();
		address.street = "ETagDoesntMatchExceptionUnitTests";
		address.zipCode = "Bar";

		User thomas = new User();
		thomas.firstname = "Thomas";
		thomas.lastname = "Darimont";
		thomas.address = address;

		userRepository.save(thomas);

		User oliver = new User();
		oliver.firstname = "Oliver";
		oliver.lastname = "Gierke";
		oliver.address = address;
		oliver.colleagues = Arrays.asList(thomas);
		userRepository.save(oliver);

		thomas.colleagues = Arrays.asList(oliver);
		userRepository.save(thomas);
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

	/**
	 * @see DATAREST-160
	 */
	@Test
	public void returnConflictWhenConcurrentlyEditingVersionedEntity() throws Exception {

		Link receiptLink = client.discoverUnique("receipts");

		Receipt receipt = new Receipt();
		receipt.amount = new BigDecimal(50);
		receipt.saleItem = "Springy Tacos";

		String stringReceipt = mapper.writeValueAsString(receipt);

		MockHttpServletResponse createdReceipt = postAndGet(receiptLink, stringReceipt, MediaType.APPLICATION_JSON);
		Link tacosLink = client.assertHasLinkWithRel("self", createdReceipt);
		assertJsonPathEquals("$.saleItem", "Springy Tacos", createdReceipt);

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(tacosLink.getHref());
		String concurrencyTag = createdReceipt.getHeader("ETag");

		mvc.perform(
				patch(builder.build().toUriString()).content("{ \"saleItem\" : \"SpringyBurritos\" }")
						.contentType(MediaType.APPLICATION_JSON).header("If-Match", concurrencyTag)).andExpect(
				status().is2xxSuccessful());

		mvc.perform(
				patch(builder.build().toUriString()).content("{ \"saleItem\" : \"SpringyTequila\" }")
						.contentType(MediaType.APPLICATION_JSON).header("If-Match", concurrencyTag)).andExpect(
				status().isPreconditionFailed());
	}

	/**
	 * @see DATAREST-471
	 */
	@Test
	public void auditableResourceHasLastModifiedHeaderSet() throws Exception {

		Profile profile = repository.findAll().iterator().next();

		String header = mvc.perform(get("/profiles/{id}", profile.getId())).//
				andReturn().getResponse().getHeader("Last-Modified");

		assertThat(header, not(isEmptyOrNullString()));
	}

	/**
	 * @see DATAREST-482
	 */
	@Test
	public void putDoesNotRemoveAssociations() throws Exception {

		Link usersLink = client.discoverUnique("users");
		Link userLink = assertHasContentLinkWithRel("self", client.request(usersLink));
		Link colleaguesLink = client.assertHasLinkWithRel("colleagues", client.request(userLink));

		// Expect a user returned as colleague
		client.follow(colleaguesLink).//
				andExpect(jsonPath("$._embedded.users").exists());

		User oliver = new User();
		oliver.firstname = "Oliver";
		oliver.lastname = "Gierke";

		putAndGet(userLink, mapper.writeValueAsString(oliver), MediaType.APPLICATION_JSON);

		// Expect colleague still present but address has been wiped
		client.follow(colleaguesLink).//
				andExpect(jsonPath("$._embedded.users").exists()).//
				andExpect(jsonPath("$.embedded.users[0].address").doesNotExist());
	}

	/**
	 * @see DATAREST-482
	 */
	@Test
	public void emptiesAssociationForEmptyUriList() throws Exception {

		Link usersLink = client.discoverUnique("users");
		Link userLink = assertHasContentLinkWithRel("self", client.request(usersLink));
		Link colleaguesLink = client.assertHasLinkWithRel("colleagues", client.request(userLink));

		putAndGet(colleaguesLink, "", MediaType.parseMediaType("text/uri-list"));

		client.follow(colleaguesLink).//
				andExpect(status().isOk()).//
				andExpect(jsonPath("$").exists());
	}

	/**
	 * @see DATAREST-491
	 */
	@Test
	public void updatesMapPropertyCorrectly() throws Exception {

		Link profilesLink = client.discoverUnique("profiles");
		Link profileLink = assertHasContentLinkWithRel("self", client.request(profilesLink));

		Profile profile = new Profile();
		profile.setMetadata(Collections.singletonMap("Key", "Value"));

		putAndGet(profileLink, mapper.writeValueAsString(profile), MediaType.APPLICATION_JSON);

		client.follow(profileLink).andExpect(jsonPath("$.metadata.Key").value("Value"));
	}

	/**
	 * @see DATAREST-506
	 */
	@Test
	public void supportsConditionalGetsOnItemResource() throws Exception {

		Receipt receipt = new Receipt();
		receipt.amount = new BigDecimal(50);
		receipt.saleItem = "Springy Tacos";

		Link receiptsLink = client.discoverUnique("receipts");

		MockHttpServletResponse response = postAndGet(receiptsLink, mapper.writeValueAsString(receipt),
				MediaType.APPLICATION_JSON);

		Link receiptLink = client.getDiscoverer(response).findLinkWithRel("self", response.getContentAsString());

		mvc.perform(get(receiptLink.getHref()).header(IF_MODIFIED_SINCE, response.getHeader(LAST_MODIFIED))).//
				andExpect(status().isNotModified()).//
				andExpect(header().string(ETAG, is(notNullValue())));

		mvc.perform(get(receiptLink.getHref()).header(IF_NONE_MATCH, response.getHeader(ETAG))).//
				andExpect(status().isNotModified()).//
				andExpect(header().string(ETAG, is(notNullValue())));
	}

	/**
	 * @see DATAREST-511
	 */
	@Test
	public void invokesQueryResourceReturningAnOptional() throws Exception {

		Profile profile = repository.findAll().iterator().next();

		Link link = client.discoverUnique("profiles", "search", "findById");

		mvc.perform(get(link.expand(profile.getId()).getHref())).//
				andExpect(status().isOk());
	}

	/**
	 * @see DATAREST-517
	 */
	@Test
	public void returnsNotFoundIfQueryExecutionDoesNotReturnResult() throws Exception {

		Link link = client.discoverUnique("profiles", "search", "findById");

		mvc.perform(get(link.expand("").getHref())).//
				andExpect(status().isNotFound());
	}
}
