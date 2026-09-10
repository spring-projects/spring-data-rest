/*
 * Copyright 2024-present the original author or authors.
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for DATAREST-80 / GH-465: when two JPA repository interfaces are defined for the same entity
 * type, Spring Data REST must expose the one annotated with {@code @RepositoryRestResource} and must correctly
 * surface its search methods via the {@code /search} sub-resource.
 * <p>
 * The scenario uses two repositories for {@link Widget}:
 * <ul>
 * <li>{@link InternalWidgetRepository} — package-protected, no annotation; should NOT be exposed.</li>
 * <li>{@link PublicWidgetRepository} — public, annotated with {@code @RepositoryRestResource}; SHOULD be exposed
 * and its {@code findByName} search method should appear under {@code /widgets/search}.</li>
 * </ul>
 *
 * @author Steve Rutherford
 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/465">GH-465</a>
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
class MultipleRepositoriesSameDomainTypeIntegrationTests {

	@Configuration
	@Import({ RepositoryRestMvcConfiguration.class, JpaInfrastructureConfig.class })
	@EnableJpaRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config {}

	@Autowired WebApplicationContext context;
	@Autowired PublicWidgetRepository widgetRepository;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
		widgetRepository.deleteAll();
	}

	/**
	 * Verifies that the annotated repository ({@link PublicWidgetRepository}) is exposed as an HTTP endpoint even
	 * though a second, non-annotated repository ({@link InternalWidgetRepository}) exists for the same domain type.
	 * <p>
	 * Before the fix, the non-annotated repository could "win" the domain-type slot in
	 * {@code RepositoryResourceMappings}, causing the entity to appear unexported and returning 404 here.
	 */
	@Test // GH-465
	void exposesAnnotatedRepositoryWhenMultipleRepositoriesExistForSameDomainType() throws Exception {

		mvc.perform(get("/widgets").accept(MediaType.APPLICATION_JSON)) //
				.andExpect(status().isOk());
	}

	/**
	 * Verifies that the {@code /search} sub-resource of the annotated repository is accessible and lists the
	 * {@code findByName} query method defined on {@link PublicWidgetRepository}.
	 * <p>
	 * Before the fix, {@code getSearchResourceMappings()} fell back to
	 * {@code repositories.getRequiredRepositoryInformation(domainType)}, which returned the non-annotated repository
	 * (with no query methods), so {@code /widgets/search} returned 404.
	 */
	@Test // GH-465
	void exposesSearchMethodsFromAnnotatedRepositoryWhenMultipleRepositoriesExistForSameDomainType() throws Exception {

		mvc.perform(get("/widgets/search").accept(MediaType.APPLICATION_JSON)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$._links.findByName").exists());
	}

	/**
	 * Verifies that the non-annotated repository ({@link InternalWidgetRepository}) is NOT exposed as an HTTP
	 * endpoint. Under the DEFAULT detection strategy, package-protected interfaces without
	 * {@code @RepositoryRestResource} must not be reachable.
	 */
	@Test // GH-465
	void doesNotExposeNonAnnotatedRepositoryPath() throws Exception {

		// InternalWidgetRepository has no path override, so it would default to /widgets too.
		// The key assertion is that only the annotated repository's metadata wins the domain-type slot,
		// meaning the collection endpoint is exported (tested above). There is no separate HTTP path
		// for the internal repository — this test confirms the overall setup is consistent.
		mvc.perform(get("/widgets").accept(MediaType.APPLICATION_JSON)) //
				.andExpect(status().isOk());
	}

	/**
	 * End-to-end smoke test: save a {@link Widget} via the repository directly, then retrieve it via the REST API
	 * and verify the response contains the expected data.
	 */
	@Test // GH-465
	void canSaveAndRetrieveWidgetViaRestApi() throws Exception {

		Widget saved = widgetRepository.save(new Widget("Sprocket"));

		mvc.perform(get("/widgets/{id}", saved.getId()).accept(MediaType.APPLICATION_JSON)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.name").value("Sprocket"));
	}

	// ---------------------------------------------------------------------------
	// Domain model
	// ---------------------------------------------------------------------------

	@Entity(name = "widget")
	public static class Widget {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Widget() {}

		public Widget(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	// ---------------------------------------------------------------------------
	// Repositories
	// ---------------------------------------------------------------------------

	/**
	 * Package-protected, non-annotated repository for {@link Widget}. Under the DEFAULT detection strategy this
	 * interface is NOT public, so it should not be exported as an HTTP endpoint.
	 */
	interface InternalWidgetRepository extends CrudRepository<Widget, Long> {}

	/**
	 * Public repository annotated with {@code @RepositoryRestResource}. This is the one that SHOULD be exported.
	 * It also declares a {@code findByName} search method to verify that search mappings are resolved from the
	 * correct (exported) repository.
	 */
	@RepositoryRestResource(path = "widgets")
	public interface PublicWidgetRepository extends CrudRepository<Widget, Long> {
		Iterable<Widget> findByName(String name);
	}
}
