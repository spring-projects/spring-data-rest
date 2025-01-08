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
package org.springframework.data.rest.tests.mongodb;

import java.math.BigInteger;
import java.util.List;

import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author Oliver Gierke
 */
public interface UserRepository extends PagingAndSortingRepository<User, BigInteger>, CrudRepository<User, BigInteger>, QuerydslPredicateExecutor<User>,
		QuerydslBinderCustomizer<QUser> {

	List<User> findByFirstname(String firstname);

	List<User> findByColleaguesContains(@Param("colleagues") User colleague);

	@Override
	default void customize(QuerydslBindings bindings, QUser root) {
		bindings.bind(root.firstname).first((path, value) -> path.containsIgnoreCase(value));
	}
}
