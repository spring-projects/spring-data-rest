/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * A repository to manage {@link Person}s.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RepositoryRestResource(collectionResourceRel = "people", path = "people")
public interface PersonRepository extends PagingAndSortingRepository<Person, Long>, CrudRepository<Person, Long> {

	@RestResource(rel = "firstname", path = "firstname")
	Page<Person> findByFirstName(@Param("firstname") String firstName, Pageable pageable);

	@RestResource(rel = "lastname", path = "lastname")
	List<Person> findByLastName(@Param("lastname") String lastName, Sort sort);

	Person findFirstPersonByFirstName(@Param("firstname") String firstName);

	Page<Person> findByCreatedGreaterThan(@Param("date") Date date, Pageable pageable);

	@Query("select p from Person p where p.created > :date")
	Page<Person> findByCreatedUsingISO8601Date(@Param("date") @DateTimeFormat(iso = ISO.DATE_TIME) Date date,
			Pageable pageable);

	// DATAREST-1121
	@Query("select p.created from Person p where p.lastName = :lastname")
	Date findCreatedDateByLastName(@Param("lastname") String lastName);
}
