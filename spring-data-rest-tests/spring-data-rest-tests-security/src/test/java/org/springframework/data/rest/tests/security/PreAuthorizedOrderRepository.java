/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.rest.tests.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
// tag::code[]
@PreAuthorize("hasRole('ROLE_USER')") // <1>
public interface PreAuthorizedOrderRepository extends CrudRepository<Order, UUID> {

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Override
	Optional<Order> findById(UUID id);

	@PreAuthorize("hasRole('ROLE_ADMIN')") // <2>
	@Override
	void deleteById(UUID aLong);

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Override
	void delete(Order order);

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Override
	void deleteAll(Iterable<? extends Order> orders);

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Override
	void deleteAll();
}
// end::code[]
