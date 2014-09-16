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
package org.springframework.data.rest.webmvc.solr;

import java.util.Arrays;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Strobl
 */
@ContextConfiguration(classes = { SolrWebTests.MyConf.class })
public class SolrWebTests extends AbstractWebIntegrationTests {

	public static @ClassRule TemporaryFolder tempFoler = new TemporaryFolder();

	@Configuration
	@EnableSolrRepositories
	@Import(value = { SolrInfrastructureConfig.class })
	static class MyConf {

		@Bean
		String solrHomeDir() {
			return tempFoler.getRoot().getAbsolutePath();
		}

	}

	@Autowired ProductRepository repo;

	@Before
	public void setUp() {

		super.setUp();

		Product product = new Product();
		product.id = "one";
		product.name = "name-wow";
		product.categories = Arrays.asList("one", "two", "three");

		repo.save(product);
	}

	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("products");
	}
}
