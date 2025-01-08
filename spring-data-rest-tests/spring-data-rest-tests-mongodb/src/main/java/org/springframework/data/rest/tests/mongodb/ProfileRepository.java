/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.rest.tests.mongodb;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public interface ProfileRepository extends PagingAndSortingRepository<Profile, String>, CrudRepository<Profile, String> {

	List<Profile> findByType(String type);

	// DATAREST-247
	long countByType(@Param("type") String type);

	// DATAREST-511
	Optional<Profile> findProfileById(@Param("id") String id);
}
