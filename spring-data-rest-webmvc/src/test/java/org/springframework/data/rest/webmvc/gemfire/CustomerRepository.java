/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.webmvc.gemfire;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface to access {@link Customer}s.
 * 
 * @author Oliver Gierke
 * @author David Turanski
 */
public interface CustomerRepository extends CrudRepository<Customer, Long> {

	/**
	 * Finds all {@link Customer}s with the given lastname.
	 * 
	 * @param lastname
	 * @return
	 */
	List<Customer> findByLastname(@Param("lastname") String lastname);

	/**
	 * Finds the Customer with the given {@link EmailAddress}.
	 * 
	 * @param emailAddress
	 * @return
	 */
	Customer findByEmailAddress(@Param("email") EmailAddress emailAddress);
}
