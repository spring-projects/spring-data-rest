/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.webmvc.neo4j;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Integration tests for exposed Neo4j repositories.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration
public class Neo4jWebTests extends AbstractWebIntegrationTests {

	@Configuration
	@ComponentScan
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class TestConfig extends Neo4jConfiguration {

		@Bean(destroyMethod = "shutdown")
		public GraphDatabaseService graphDatabaseService() {
			return new GraphDatabaseFactory().newEmbeddedDatabase("target/graphdb");
		}
	}

	@Autowired TestDataPopulator populator;

	@Before
	@Override
	public void setUp() {
		this.populator.populate();
		super.setUp();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("customers");
	}

	/**
	 * @see DATAREST-184
	 */
	@Test
	public void deletesCustomer() throws Exception {

		// Lookup customer
		Link customers = discoverUnique("customers");
		Link customerLink = assertHasContentLinkWithRel("self", request(customers));

		// Delete customer
		mvc.perform(delete(customerLink.getHref()));

		// Assert no customers anymore
		assertDoesNotHaveContentLinkWithRel("self", request(customers));
	}
}
