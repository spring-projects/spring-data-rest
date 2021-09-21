/*
 * Copyright 2016-2018 original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for projections.
 *
 * @author Oliver Gierke
 * @author Craig Andrews
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ShopConfiguration.class)
class ShopIntegrationTests extends AbstractWebIntegrationTests {

	@Test
	void rendersRepresentationCorrectly() throws Exception {

		Link ordersLink = client.discoverUnique("orders");
		String selfLink = JsonPath.parse(client.request(ordersLink).getContentAsString())
				.read("$._embedded.orders[0]._links.self.href");

		ResultActions actions = client.follow(selfLink)
				// Lookup type is rendered as String
				.andExpect(jsonPath("items[0].type", is("good")))//
				// Invokes RepresentationModelProcessor for nested Resource
				.andExpect(jsonPath("items[0]._links.bar").exists());//

		// Adds excerpt projection for related product
		expectRelatedResource("items[0].product", actions);
		expectRelatedResource("items[0].products", actions);

		// Adds excerpt projection for root level reference
		expectRelatedResource("customer", actions);
	}

	@Test // DATAREST-221
	void renderProductNameOnlyProjection() throws Exception {

		Map<String, Object> arguments = Collections.singletonMap("projection", "nameOnly");

		client.follow(client.discoverUnique("products").expand(arguments))//
				.andExpect(status().isOk())//
				.andDo(MockMvcResultHandlers.print()) //
				.andExpect(jsonPath("$._embedded.products[0].name", notNullValue()))//
				.andExpect(jsonPath("$._embedded.products[0].price").doesNotExist());
	}

	@Test // DATAREST-221
	void renderProductNameOnlyProjectionResourceProcessor() throws Exception {

		Map<String, Object> arguments = Collections.singletonMap("projection", "nameOnly");

		client.follow(client.discoverUnique("products").expand(arguments))//
				.andExpect(status().isOk())//
				.andExpect(jsonPath("$._embedded.products[0]._links.beta").exists());
	}

	@Test // DATAREST-221
	void renderOrderItemsOnlyProjectionResourceProcessor() throws Exception {

		Map<String, Object> arguments = Collections.singletonMap("projection", "itemsOnly");

		client.follow(client.discoverUnique("orders").expand(arguments))//
				.andExpect(status().isOk())//
				.andExpect(jsonPath("$._embedded.orders[0].items[0].products[0].name").exists())//
				.andExpect(jsonPath("$._embedded.orders[0].items[0].products[0]._links.beta").exists());
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
