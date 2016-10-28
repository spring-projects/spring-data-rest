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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.Collections;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import net.minidev.json.JSONArray;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.tests.CommonWebTests;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Angrish
 */
@Transactional
@ContextConfiguration(classes = Neo4jConfig.class)
public class Neo4jWebTests extends CommonWebTests {

    @Autowired
    TestDataPopulator populator;

    @Before
    @Override
    public void setUp() {
        this.populator.populateRepositories();
        super.setUp();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
     */
    @Override
    protected Iterable<String> expectedRootLinkRels() {
        return Arrays.asList("customers", "countries");
    }

    @Test
    public void emptyObjectMatchesResponse() throws Exception {

        Link customersLink = client.discoverUnique("customers");
        Link customerLink = assertHasContentLinkWithRel("self", client.request(customersLink));

        MockHttpServletResponse response = patchAndGet(customerLink,
                "{\"firstName\" : null, \"lastName\" : null, \"emailAddress\": null, \"addresses\" : [{ \"street\" : null, \"city\": null, \"country\": null}]}", MediaType.APPLICATION_JSON);

        assertThat(JsonPath.read(response.getContentAsString(), "$.firstName"), is(nullValue()));
        assertThat(JsonPath.read(response.getContentAsString(), "$.lastName"), is(nullValue()));
        assertThat(JsonPath.read(response.getContentAsString(), "$.emailAddress"), is(nullValue()));
        assertThat(JsonPath.read(response.getContentAsString(), "$.addresses[0].street"), is(nullValue()));
        assertThat(JsonPath.read(response.getContentAsString(), "$.addresses[0].city"), is(nullValue()));
        assertThat(JsonPath.read(response.getContentAsString(), "$.addresses[0].country"), is(nullValue()));
    }

    @Test
    public void customersLinkContainsAllCustomers() throws Exception {

        Link profileLink = client.discoverUnique("customers");
        client.follow(profileLink).//
                andExpect(jsonPath("$._embedded.customers").value(hasSize(7)));
    }

    @Test
    public void deletedCustomerIsRemovedFromEndpoint() throws Exception {

        // Lookup customer
        Link customers = client.discoverUnique("customers");
        Link customerLink = assertHasContentLinkWithRel("self", client.request(customers));

        // Delete customer
        mvc.perform(delete(customerLink.getHref()));

        // Assert we deleted a customer.
        client.follow(customers).//
                andExpect(jsonPath("$._embedded.customers").value(hasSize(6)));
    }


}
