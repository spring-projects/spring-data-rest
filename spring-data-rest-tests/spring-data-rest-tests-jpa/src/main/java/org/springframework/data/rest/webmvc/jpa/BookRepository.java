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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RepositoryRestResource(excerptProjection = BookExcerpt.class)
public interface BookRepository extends CrudRepository<Book, Long> {

	@RestResource(rel = "find-by-sorted")
	List<Book> findBy(Sort sort);

	@Query("select b from Book b where :author member of b.authors")
	List<Book> findByAuthorsContains(@Param("author") Author author);

	@RestResource(rel = "find-spring-books-sorted")
	@Query("select b from Book b where b.title like 'Spring%'")
	Page<Book> findByTitleIsLike(Pageable pageable);
}
