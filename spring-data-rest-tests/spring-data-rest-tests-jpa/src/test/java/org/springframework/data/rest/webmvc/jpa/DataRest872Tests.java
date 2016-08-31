/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.tests.AbstractWebIntegrationTests;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

/**
 * Integration tests for DATAREST-872.
 * 
 * @author Mathias D
 * @author Rob Baily
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(
		classes = { JpaRepositoryConfig.class, RepositoryRestMvcConfiguration.class })
public class DataRest872Tests extends AbstractWebIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired ParentRepository parentRepository;
	private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

	@Before
	@Override
	public void setUp() {
		super.setUp();

		Parent parent = new Parent();
		DecimalValue decimalValue = new DecimalValue();
		decimalValue.setValue(BigDecimal.TEN);
		parent.setValueHolder(decimalValue);
		parentRepository.save(parent);

		parent = new Parent();
		StringValue stringValue = new StringValue();
		stringValue.setValue("some");
		parent.setValueHolder(stringValue);
		parentRepository.save(parent);
	}

	/**
	 * @see DATAREST-872
	 */
	@Test
	public void testParentsRetrieval() throws Exception {
		mvc.perform(
				get(client.discoverUnique("parents").expand().getHref())
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
		;
	}
}
