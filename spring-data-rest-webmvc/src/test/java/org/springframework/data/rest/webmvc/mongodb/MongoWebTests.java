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
package org.springframework.data.rest.webmvc.mongodb;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author Oliver Gierke
 */
@ContextConfiguration(classes = MongoDbRepositoryConfig.class)
public class MongoWebTests extends AbstractWebIntegrationTests {

	@Autowired ProfileRepository repository;
	
	@Before
	public void populateProfiles() {
		
		Profile twitter = new Profile();
		twitter.setPerson(1L);
		twitter.setType("Twitter");
		
		Profile linkedIn = new Profile();
		linkedIn.setPerson(1L);
		linkedIn.setType("LinkedIn");
		
		repository.save(Arrays.asList(twitter, linkedIn));
	}
	
	@After
	public void cleanUp() {
		repository.deleteAll();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Arrays.asList("profile");
	}
	
	@Test
	public void foo() throws Exception {
		
		Link profileLink = discoverUnique("profile");
		follow(profileLink).andExpect(jsonPath("$.content").value(hasSize(2)));	
	}
}
