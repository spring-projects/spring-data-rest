/*
 * Copyright 2012-present the original author or authors.
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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository for the abstract {@link Meal} base class, used to verify that Spring Data REST correctly generates
 * self-links and serializes subclass-specific fields when the repository domain type is an abstract entity with JPA
 * inheritance (DATAREST-1080 / GitHub issue #1445).
 *
 * @author Steve Rutherford
 */
@RepositoryRestResource(path = "meals", collectionResourceRel = "meals", itemResourceRel = "meal")
public interface MealRepository extends JpaRepository<Meal, Long> {}
