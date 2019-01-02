/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Test configuration for JPA.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
public class JpaRepositoryConfig extends JpaInfrastructureConfig {

	@Bean
	public BookIdConverter bookIdConverter() {
		return new BookIdConverter();
	}

	@Bean
	public TestDataPopulator testDataPopulator() {
		return new TestDataPopulator();
	}

	@BasePathAwareController
	static class BooksHtmlController {

		@RequestMapping(value = "/books/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
		void someMethod(@PathVariable String id) {}
	}

	@RepositoryRestController
	@RequestMapping("orders")
	static class OrdersJsonController {

		@RequestMapping(value = "/search/sort", method = RequestMethod.POST, produces = "application/hal+json")
		void someMethodWithArgs(Sort sort, Pageable pageable, DefaultedPageable defaultedPageable) {}
	}
}
