/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.data.rest.core.domain;

import java.util.Date;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * A repository to manage {@link Person}s.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RepositoryRestResource(collectionResourceRel = "people", path = "people")
public interface PersonRepository extends PagingAndSortingRepository<Person, UUID> {

	@RestResource(rel = "firstname", path = "firstname")
	Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

	Page<Person> findByCreatedGreaterThan(@Param("date") Date date, Pageable pageable);

	/**
	 * This method matches the earlier one, causing an ambiguous mapping except for the exported
	 *      setting
	 */
	// DATAREST-107
	@RestResource(rel = "firstname", path = "firstname", exported = false)
	Person findByFirstName(@Param("firstName") String firstName);
}
