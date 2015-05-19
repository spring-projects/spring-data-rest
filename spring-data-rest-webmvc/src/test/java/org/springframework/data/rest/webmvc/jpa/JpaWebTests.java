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
package org.springframework.data.rest.webmvc.jpa;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.webmvc.util.TestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.CommonWebTests;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RelProvider;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Web integration tests specific to JPA.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
@Transactional
@ContextConfiguration(classes = JpaRepositoryConfig.class)
public class JpaWebTests extends CommonWebTests {

	private static final MediaType TEXT_URI_LIST = MediaType.valueOf("text/uri-list");
	static final String LINK_TO_SIBLINGS_OF = "$._embedded..[?(@.firstName == '%s')]._links.siblings.href[0]";

	@Autowired TestDataPopulator loader;
	@Autowired ResourceMappings mappings;
	@Autowired RelProvider relProvider;

	ObjectMapper mapper = new ObjectMapper();

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#setUp()
	 */
	@Override
	@Before
	public void setUp() {
		loader.populateRepositories();
		super.setUp();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("people", "authors", "books");
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#getPayloadToPost()
	 */
	@Override
	protected Map<String, String> getPayloadToPost() throws Exception {
		return Collections.singletonMap("people", readFileFromClasspath("person.json"));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#getRootAndLinkedResources()
	 */
	@Override
	protected MultiValueMap<String, String> getRootAndLinkedResources() {

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("authors", "books");
		map.add("books", "authors");

		return map;
	}

	/**
	 * @see DATAREST-99
	 */
	@Test
	public void doesNotExposeCreditCardRepository() throws Exception {

		mvc.perform(get("/")). //
				andExpect(status().isOk()). //
				andExpect(doesNotHaveLinkWithRel(mappings.getMetadataFor(CreditCard.class).getRel()));
	}

	@Test
	public void accessPersons() throws Exception {

		MockHttpServletResponse response = client.request("/people?page=0&size=1");

		Link nextLink = client.assertHasLinkWithRel(Link.REL_NEXT, response);
		assertDoesNotHaveLinkWithRel(Link.REL_PREVIOUS, response);

		response = client.request(nextLink);
		client.assertHasLinkWithRel(Link.REL_PREVIOUS, response);
		nextLink = client.assertHasLinkWithRel(Link.REL_NEXT, response);

		response = client.request(nextLink);
		client.assertHasLinkWithRel(Link.REL_PREVIOUS, response);
		assertDoesNotHaveLinkWithRel(Link.REL_NEXT, response);
	}

	/**
	 * @see DATAREST-169
	 */
	@Test
	public void exposesLinkForRelatedResource() throws Exception {

		MockHttpServletResponse response = client.request("/");
		Link ordersLink = client.assertHasLinkWithRel("orders", response);

		MockHttpServletResponse orders = client.request(ordersLink);
		Link creatorLink = assertHasContentLinkWithRel("creator", orders);

		assertThat(client.request(creatorLink), is(notNullValue()));
	}

	/**
	 * @see DATAREST-200
	 */
	@Test
	public void exposesInlinedEntities() throws Exception {

		MockHttpServletResponse response = client.request("/");
		Link ordersLink = client.assertHasLinkWithRel("orders", response);

		MockHttpServletResponse orders = client.request(ordersLink);
		assertHasJsonPathValue("$..lineItems", orders);
	}

	/**
	 * @see DATAREST-199
	 */
	@Test
	public void createsOrderUsingPut() throws Exception {

		mvc.perform(//
				put("/orders/{id}", 4711).//
						content(readFileFromClasspath("order.json")).contentType(MediaType.APPLICATION_JSON)//
		).andExpect(status().isCreated());
	}

	/**
	 * @see DATAREST-117
	 */
	@Test
	public void createPersonThenVerifyIgnoredAttributesDontExist() throws Exception {

		Link peopleLink = client.discoverUnique("people");
		ObjectMapper mapper = new ObjectMapper();
		Person frodo = new Person("Frodo", "Baggins");
		frodo.setAge(77);
		frodo.setHeight(42);
		frodo.setWeight(75);
		String frodoString = mapper.writeValueAsString(frodo);

		MockHttpServletResponse response = postAndGet(peopleLink, frodoString, MediaType.APPLICATION_JSON);

		assertJsonPathEquals("$.firstName", "Frodo", response);
		assertJsonPathEquals("$.lastName", "Baggins", response);
		assertJsonPathDoesntExist("$.age", response);
		assertJsonPathDoesntExist("$.height", response);
		assertJsonPathDoesntExist("$.weight", response);
	}

	/**
	 * @see DATAREST-95
	 */
	@Test
	public void createThenPatch() throws Exception {

		Link peopleLink = client.discoverUnique("people");

		MockHttpServletResponse bilbo = postAndGet(peopleLink, "{ \"firstName\" : \"Bilbo\", \"lastName\" : \"Baggins\" }",
				MediaType.APPLICATION_JSON);

		Link bilboLink = client.assertHasLinkWithRel("self", bilbo);

		assertThat((String) JsonPath.read(bilbo.getContentAsString(), "$.firstName"), is("Bilbo"));
		assertThat((String) JsonPath.read(bilbo.getContentAsString(), "$.lastName"), is("Baggins"));

		MockHttpServletResponse frodo = patchAndGet(bilboLink, "{ \"firstName\" : \"Frodo\" }", MediaType.APPLICATION_JSON);

		assertThat((String) JsonPath.read(frodo.getContentAsString(), "$.firstName"), is("Frodo"));
		assertThat((String) JsonPath.read(frodo.getContentAsString(), "$.lastName"), is("Baggins"));

		frodo = patchAndGet(bilboLink, "{ \"firstName\" : null }", MediaType.APPLICATION_JSON);

		assertThat((String) JsonPath.read(frodo.getContentAsString(), "$.firstName"), is(nullValue()));
		assertThat((String) JsonPath.read(frodo.getContentAsString(), "$.lastName"), is("Baggins"));
	}

	/**
	 * @see DATAREST-150
	 */
	@Test
	public void createThenPut() throws Exception {

		Link peopleLink = client.discoverUnique("people");

		MockHttpServletResponse bilbo = postAndGet(peopleLink,//
				"{ \"firstName\" : \"Bilbo\", \"lastName\" : \"Baggins\" }",//
				MediaType.APPLICATION_JSON);

		Link bilboLink = client.assertHasLinkWithRel("self", bilbo);

		assertThat((String) JsonPath.read(bilbo.getContentAsString(), "$.firstName"), equalTo("Bilbo"));
		assertThat((String) JsonPath.read(bilbo.getContentAsString(), "$.lastName"), equalTo("Baggins"));

		MockHttpServletResponse frodo = putAndGet(bilboLink,//
				"{ \"firstName\" : \"Frodo\" }",//
				MediaType.APPLICATION_JSON);

		assertThat((String) JsonPath.read(frodo.getContentAsString(), "$.firstName"), equalTo("Frodo"));
		assertNull(JsonPath.read(frodo.getContentAsString(), "$.lastName"));
	}

	@Test
	public void listsSiblingsWithContentCorrectly() throws Exception {
		assertPersonWithNameAndSiblingLink("John");
	}

	@Test
	public void listsEmptySiblingsCorrectly() throws Exception {
		assertPersonWithNameAndSiblingLink("Billy Bob");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithMultiplePosts() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingLink = links.get(0);

		patchAndGet(frodosSiblingLink, links.get(1).getHref(), TEXT_URI_LIST);
		patchAndGet(frodosSiblingLink, links.get(2).getHref(), TEXT_URI_LIST);
		patchAndGet(frodosSiblingLink, links.get(3).getHref(), TEXT_URI_LIST);

		assertSiblingNames(frodosSiblingLink, "Bilbo", "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithSinglePost() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingLink = links.get(0);

		patchAndGet(frodosSiblingLink, toUriList(links.get(1), links.get(2), links.get(3)), TEXT_URI_LIST);

		assertSiblingNames(frodosSiblingLink, "Bilbo", "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithMultiplePuts() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingsLink = links.get(0);

		putAndGet(frodosSiblingsLink, links.get(1).expand().getHref(), TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, links.get(2).expand().getHref(), TEXT_URI_LIST);
		putAndGet(frodosSiblingsLink, links.get(3).expand().getHref(), TEXT_URI_LIST);
		assertSiblingNames(frodosSiblingsLink, "Pippin");

		patchAndGet(frodosSiblingsLink, links.get(2).getHref(), TEXT_URI_LIST);
		assertSiblingNames(frodosSiblingsLink, "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithSinglePut() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodoSiblingLink = links.get(0);

		putAndGet(frodoSiblingLink, toUriList(links.get(1), links.get(2), links.get(3)), TEXT_URI_LIST);
		assertSiblingNames(frodoSiblingLink, "Bilbo", "Merry", "Pippin");

		putAndGet(frodoSiblingLink, toUriList(links.get(3)), TEXT_URI_LIST);
		assertSiblingNames(frodoSiblingLink, "Pippin");

		patchAndGet(frodoSiblingLink, toUriList(links.get(2)), TEXT_URI_LIST);
		assertSiblingNames(frodoSiblingLink, "Merry", "Pippin");
	}

	/**
	 * @see DATAREST-219
	 */
	@Test
	public void manipulatePropertyCollectionRestfullyWithDelete() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"), //
				new Person("Merry", "Baggins"), //
				new Person("Pippin", "Baggins"));

		Link frodosSiblingsLink = links.get(0);

		patchAndGet(frodosSiblingsLink, links.get(1).getHref(), TEXT_URI_LIST);
		patchAndGet(frodosSiblingsLink, links.get(2).getHref(), TEXT_URI_LIST);
		patchAndGet(frodosSiblingsLink, links.get(3).getHref(), TEXT_URI_LIST);

		String pippinId = new UriTemplate("/people/{id}").match(links.get(3).getHref()).get("id");
		deleteAndVerify(new Link(frodosSiblingsLink.getHref() + "/" + pippinId));

		assertSiblingNames(frodosSiblingsLink, "Bilbo", "Merry");
	}

	/**
	 * @see DATAREST-50
	 */
	@Test
	public void propertiesCanHaveNulls() throws Exception {

		Link peopleLink = client.discoverUnique("people");

		Person frodo = new Person();
		frodo.setFirstName("Frodo");
		frodo.setLastName(null);

		MockHttpServletResponse response = postAndGet(peopleLink, mapper.writeValueAsString(frodo),
				MediaType.APPLICATION_JSON);
		String responseBody = response.getContentAsString();

		assertEquals(JsonPath.read(responseBody, "$.firstName"), "Frodo");
		assertNull(JsonPath.read(responseBody, "$.lastName"));
	}

	/**
	 * @see DATAREST-238
	 */
	@Test
	public void putShouldWorkDespiteExistingLinks() throws Exception {

		Link peopleLink = client.discoverUnique("people");

		Person frodo = new Person("Frodo", "Baggins");
		String frodoString = mapper.writeValueAsString(frodo);

		MockHttpServletResponse createdPerson = postAndGet(peopleLink, frodoString, MediaType.APPLICATION_JSON);

		Link frodoLink = client.assertHasLinkWithRel("self", createdPerson);
		assertJsonPathEquals("$.firstName", "Frodo", createdPerson);

		String bilboWithFrodosLinks = createdPerson.getContentAsString().replace("Frodo", "Bilbo");

		MockHttpServletResponse overwrittenResponse = putAndGet(frodoLink, bilboWithFrodosLinks, MediaType.APPLICATION_JSON);

		client.assertHasLinkWithRel("self", overwrittenResponse);
		assertJsonPathEquals("$.firstName", "Bilbo", overwrittenResponse);
	}

	/**
	 * @see DATAREST-217
	 */
	@Test
	public void doesNotAllowGetToCollectionResourceIfFindAllIsNotExported() throws Exception {

		Link link = client.discoverUnique("addresses");

		mvc.perform(get(link.getHref())).//
				andExpect(status().isMethodNotAllowed());
	}

	/**
	 * @see DATAREST-217
	 */
	@Test
	public void doesNotAllowPostToCollectionResourceIfSaveIsNotExported() throws Exception {

		Link link = client.discoverUnique("addresses");

		mvc.perform(post(link.getHref()).content("{}").contentType(MediaType.APPLICATION_JSON)).//
				andExpect(status().isMethodNotAllowed());
	}

	/**
	 * Checks, that the server only returns the properties contained in the projection requested.
	 * 
	 * @see OrderSummary
	 * @see DATAREST-221
	 */
	@Test
	public void returnsProjectionIfRequested() throws Exception {

		Link orders = client.discoverUnique("orders");

		MockHttpServletResponse response = client.request(orders);
		Link orderLink = assertContentLinkWithRel("self", response, true).expand();

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(orderLink.getHref());
		String uri = builder.queryParam("projection", "summary").build().toUriString();

		response = mvc.perform(get(uri)). //
				andExpect(status().isOk()). //
				andExpect(jsonPath("$.price", is(2.5))).//
				andReturn().getResponse();

		assertJsonPathDoesntExist("$.lineItems", response);
	}

	/**
	 * @see DATAREST-261
	 */
	@Test
	public void relProviderDetectsCustomizedMapping() {
		assertThat(relProvider.getCollectionResourceRelFor(Person.class), is("people"));
	}

	/**
	 * @see DATAREST-311
	 */
	@Test
	public void onlyLinksShouldAppearWhenExecuteSearchCompact() throws Exception {

		Link peopleLink = client.discoverUnique("people");
		Person daenerys = new Person("Daenerys", "Targaryen");
		String daenerysString = mapper.writeValueAsString(daenerys);

		MockHttpServletResponse createdPerson = postAndGet(peopleLink, daenerysString, MediaType.APPLICATION_JSON);
		Link daenerysLink = client.assertHasLinkWithRel("self", createdPerson);
		assertJsonPathEquals("$.firstName", "Daenerys", createdPerson);

		Link searchLink = client.discoverUnique(peopleLink, "search");
		Link byFirstNameLink = client.discoverUnique(searchLink, "findFirstPersonByFirstName");

		MockHttpServletResponse response = client.request(byFirstNameLink.expand("Daenerys"),
				MediaType.parseMediaType("application/x-spring-data-compact+json"));

		String responseBody = response.getContentAsString();

		JSONArray personLinks = JsonPath.<JSONArray> read(responseBody, "$.links[?(@.rel=='person')].href");

		assertThat(personLinks, hasSize(1));
		assertThat(personLinks.get(0), is((Object) daenerysLink.getHref()));
		assertThat(JsonPath.<JSONArray> read(responseBody, "$.content"), hasSize(0));
	}

	/**
	 * @see DATAREST-317
	 */
	@Test
	public void rendersExcerptProjectionsCorrectly() throws Exception {

		Link authorsLink = client.discoverUnique("authors");

		MockHttpServletResponse response = client.request(authorsLink);
		String firstAuthorPath = "$._embedded.authors[0]";

		// Has main content
		assertHasJsonPathValue(firstAuthorPath.concat(".name"), response);

		// Embeddes content of related entity, self link and keeps relation link
		assertHasJsonPathValue(firstAuthorPath.concat("._embedded.books[0].title"), response);
		assertHasJsonPathValue(firstAuthorPath.concat("._embedded.books[0]._links.self"), response);
		assertHasJsonPathValue(firstAuthorPath.concat("._links.books"), response);

		// Access item resource and expect link to related resource present
		String content = response.getContentAsString();
		String href = JsonPath.read(content, firstAuthorPath.concat("._links.self.href"));

		client.follow(new Link(href)).andExpect(client.hasLinkWithRel("books"));
	}

	/**
	 * @see DATAREST-353
	 */
	@Test
	public void returns404WhenTryingToDeleteANonExistingResource() throws Exception {

		Link receiptsLink = client.discoverUnique("receipts");

		mvc.perform(delete(receiptsLink.getHref().concat("/{id}"), 4711)).//
				andExpect(status().isNotFound());
	}

	/**
	 * @see DATAREST-384
	 */
	@Test
	public void execturesSearchThatTakesASort() throws Exception {

		Link booksLink = client.discoverUnique("books");
		Link searchLink = client.discoverUnique(booksLink, "search");
		Link findBySortedLink = client.discoverUnique(searchLink, "find-by-sorted");

		// Assert sort options advertised
		assertThat(findBySortedLink.isTemplated(), is(true));
		assertThat(findBySortedLink.getVariableNames(), hasItems("sort", "projection"));

		// Assert results returned as specified
		client.follow(findBySortedLink.expand("title,desc")).//
				andExpect(jsonPath("$._embedded.books[0].title").value("Spring Data (Second Edition)")).//
				andExpect(jsonPath("$._embedded.books[1].title").value("Spring Data")).//
				andExpect(client.hasLinkWithRel("self"));

		client.follow(findBySortedLink.expand("title,asc")).//
				andExpect(jsonPath("$._embedded.books[0].title").value("Spring Data")).//
				andExpect(jsonPath("$._embedded.books[1].title").value("Spring Data (Second Edition)")).//
				andExpect(client.hasLinkWithRel("self"));
	}

	/**
	 * @see DATAREST-160
	 */
	@Test
	public void returnConflictWhenConcurrentlyEditingVersionedEntity() throws Exception {

		Link receiptLink = client.discoverUnique("receipts");

		Receipt receipt = new Receipt();
		receipt.setAmount(new BigDecimal(50));
		receipt.setSaleItem("Springy Tacos");

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
						.contentType(MediaType.APPLICATION_JSON).header("If-Match", "\"falseETag\"")).andExpect(
				status().isPreconditionFailed());
	}

	/**
	 * @see DATAREST-423
	 */
	@Test
	public void invokesCustomControllerAndBindsDomainObjectCorrectly() throws Exception {

		MockHttpServletResponse authorsResponse = client.request(client.discoverUnique("authors"));

		String authorUri = JsonPath.read(authorsResponse.getContentAsString(), "$._embedded.authors[0]._links.self.href");

		mvc.perform(delete(authorUri)).//
				andExpect(status().isIAmATeapot());
	}

	/**
	 * @see DATAREST-523
	 */
	@Test
	public void augmentsCollectionAssociationUsingPost() throws Exception {

		List<Link> links = preparePersonResources(new Person("Frodo", "Baggins"), //
				new Person("Bilbo", "Baggins"));

		Link frodosSiblingsLink = links.get(0).expand();
		Link bilboLink = links.get(1);

		for (int i = 1; i <= 2; i++) {

			mvc.perform(post(frodosSiblingsLink.getHref()).//
					content(bilboLink.getHref()).//
					contentType(TEXT_URI_LIST)).//
					andExpect(status().isNoContent());

			mvc.perform(get(frodosSiblingsLink.getHref())).//
					andExpect(jsonPath("$._embedded.people", hasSize(i)));
		}
	}

	private List<Link> preparePersonResources(Person primary, Person... persons) throws Exception {

		Link peopleLink = client.discoverUnique("people");
		List<Link> links = new ArrayList<Link>();

		MockHttpServletResponse primaryResponse = postAndGet(peopleLink, mapper.writeValueAsString(primary),
				MediaType.APPLICATION_JSON);
		links.add(client.assertHasLinkWithRel("siblings", primaryResponse));

		for (Person person : persons) {

			String payload = mapper.writeValueAsString(person);
			MockHttpServletResponse response = postAndGet(peopleLink, payload, MediaType.APPLICATION_JSON);

			links.add(client.assertHasLinkWithRel(Link.REL_SELF, response));
		}

		return links;
	}

	/**
	 * Asserts the {@link Person} resource the given link points to contains siblings with the given names.
	 * 
	 * @param link
	 * @param siblingNames
	 * @throws Exception
	 */
	private void assertSiblingNames(Link link, String... siblingNames) throws Exception {

		String responseBody = client.request(link).getContentAsString();
		List<String> persons = JsonPath.read(responseBody, "$._embedded.people[*].firstName");

		assertThat(persons, hasSize(siblingNames.length));
		assertThat(persons, hasItems(siblingNames));
	}

	private void assertPersonWithNameAndSiblingLink(String name) throws Exception {

		MockHttpServletResponse response = client.request(client.discoverUnique("people"));

		String jsonPath = String.format("$._embedded.people[?(@.firstName == '%s')][0]", name);

		// Assert content inlined
		Object john = JsonPath.read(response.getContentAsString(), jsonPath);
		assertThat(john, is(notNullValue()));
		assertThat(JsonPath.read(john, "$.firstName"), is(notNullValue()));

		// Assert sibling link exposed in resource pointed to
		Link selfLink = new Link(JsonPath.<String> read(john, "$._links.self.href"));
		client.follow(selfLink).//
				andExpect(status().isOk()).//
				andExpect(jsonPath("$._links.siblings", is(notNullValue())));
	}

	private static String toUriList(Link... links) {

		List<String> uris = new ArrayList<String>(links.length);

		for (Link link : links) {
			uris.add(link.expand().getHref());
		}

		return StringUtils.collectionToDelimitedString(uris, "\n");
	}
}
