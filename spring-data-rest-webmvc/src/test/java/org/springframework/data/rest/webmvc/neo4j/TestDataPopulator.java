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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple component to populate the Neo4j repositories with sample data.
 * 
 * @author Oliver Gierke
 */
@Component
public class TestDataPopulator {

	@Autowired CustomerRepository repository;

	@Transactional
	public void populate() {

		if (repository.count() > 0) {
			return;
		}

		repository.save(new Customer("Oliver", "Gierke", "ogierke@gopivotal.com"));
	}
}
