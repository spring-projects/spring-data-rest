/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.tests.shop;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ShopConfiguration.class)
public class ShopIntegrationTests extends AbstractWebIntegrationTests {

	@Test
	public void rendersRepresentationCorrectly() throws Exception {

		Link ordersLink = client.discoverUnique("orders");
		String selfLink = JsonPath.parse(client.request(ordersLink).getContentAsString())
				.read("$._embedded.orders[0]._links.self.href");

		ResultActions actions = client.follow(selfLink)
				// Lookup type is rendered as String
				.andExpect(jsonPath("items[0].type", is("good")))//
				// Invokes ResourceProcessor for nested Resource
				.andExpect(jsonPath("items[0]._links.bar").exists());//

		// Adds excerpt projection for related product
		expectRelatedResource("items[0].product", actions);
		expectRelatedResource("items[0].products", actions);

		// Adds excerpt projection for root level reference
		expectRelatedResource("customer", actions);
	}

	private static void expectRelatedResource(String name, ResultActions actions) throws Exception {

		int dotIndex = name.lastIndexOf('.');

		String prefix = dotIndex == -1 ? "" : name.substring(0, dotIndex);
		String suffix = dotIndex == -1 ? name : name.substring(dotIndex);

		actions.andExpect(jsonPath(prefix.concat(suffix)).doesNotExist());
		actions.andExpect(jsonPath(prefix.concat("_links.").concat(suffix)).exists());
		actions.andExpect(jsonPath(prefix.concat("_embedded.").concat(suffix)).exists());
	}
}
