/*
 * Copyright 2018-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for GH-1515 (DATAREST-1195): verifies that URI-to-entity deserialization for association
 * properties works correctly when the {@code ANNOTATED} repository detection strategy is active and the target
 * repository is <em>not</em> annotated with {@code @RepositoryRestResource}.
 *
 * <h3>Scenario</h3>
 * <ul>
 *   <li>{@link ProfileRepository} is <em>not</em> annotated → not exported as an HTTP endpoint under ANNOTATED
 *       strategy.</li>
 *   <li>{@link MemberRepository} <em>is</em> annotated → exported as an HTTP endpoint.</li>
 *   <li>A POST to {@code /members} with a JSON body that references a {@link Profile} by URI (e.g.
 *       {@code "profile": "/profiles/1"}) must succeed without a {@code JsonMappingException}.</li>
 * </ul>
 *
 * <h3>Root cause (before fix)</h3>
 * {@code Associations.isLinkableAssociation()} checked {@code metadata.isExported()} for the target type. Under the
 * ANNOTATED strategy, un-annotated repositories return {@code isExported() == false}, so the check returned
 * {@code false} and the {@code UriStringDeserializer} was never registered for the {@code profile} property. Jackson
 * then fell back to its default deserializer, which cannot construct a {@link Profile} from a plain URI string.
 *
 * <h3>Fix</h3>
 * A new {@code Associations.isUriResolvableAssociation()} method is used in the deserializer modifier. It returns
 * {@code true} whenever a repository (and therefore a {@code ResourceMetadata}) exists for the target type,
 * regardless of whether that repository is exported as an HTTP endpoint.
 *
 * @author Spring Data REST team
 * @author Steve Rutherford
 * @see <a href="https://github.com/spring-projects/spring-data-rest/issues/1515">GH-1515</a>
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Transactional
@ContextConfiguration(
		classes = { JpaRepositoryConfig.class, RepositoryRestMvcConfiguration.class,
				AnnotatedStrategyUriDeserializationIntegrationTests.AnnotatedStrategyConfig.class })
class AnnotatedStrategyUriDeserializationIntegrationTests {

	@Autowired WebApplicationContext context;
	@Autowired ProfileRepository profileRepository;
	@Autowired MemberRepository memberRepository;

	MockMvc mockMvc;

	/**
	 * Configures the {@code ANNOTATED} repository detection strategy. This is the critical precondition: with this
	 * strategy, {@link ProfileRepository} (which has no {@code @RepositoryRestResource} annotation) is NOT exported as
	 * an HTTP endpoint, while {@link MemberRepository} (which IS annotated) is exported.
	 */
	@Configuration
	static class AnnotatedStrategyConfig {

		@Bean
		RepositoryRestConfigurer annotatedStrategyConfigurer() {
			return RepositoryRestConfigurer.withConfig(
					config -> config.setRepositoryDetectionStrategy(RepositoryDetectionStrategies.ANNOTATED));
		}
	}

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
				.build();
	}

	/**
	 * Regression test for GH-1515.
	 * <p>
	 * POSTs a {@link Member} payload that references a {@link Profile} by URI. Before the fix, this threw a
	 * {@code JsonMappingException} because the {@code UriStringDeserializer} was not registered for the {@code profile}
	 * property when the ANNOTATED strategy was active and {@link ProfileRepository} was not annotated.
	 */
	@Test // GH-1515
	void postMemberWithProfileUriSucceedsUnderAnnotatedDetectionStrategy() throws Exception {

		// Persist a Profile to reference by URI
		Profile profile = profileRepository.save(new Profile("Test Profile"));
		Long profileId = profile.getId();

		// Build a JSON payload that references the profile by URI — exactly as described in the issue
		String payload = String.format("""
				{
				  "username": "testuser",
				  "profile": "/profiles/%d"
				}
				""", profileId);

		// POST to /members — must succeed (2xx) without a JsonMappingException.
		// Before the fix this returned 400 Bad Request with:
		//   "JSON parse error: Can not construct instance of Profile:
		//    no String-argument constructor/factory method to deserialize from String value ('/profiles/1')"
		mockMvc.perform(post("/members")
				.content(payload)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
	}

	/**
	 * Verifies that the ANNOTATED strategy correctly suppresses the HTTP endpoint for {@link ProfileRepository}: a GET
	 * to {@code /profiles} must return 404 (the collection resource is not exposed).
	 * <p>
	 * This confirms that the fix does not accidentally re-expose the un-annotated repository as an HTTP endpoint.
	 */
	@Test // GH-1515
	void profileRepositoryIsNotExposedAsHttpEndpointUnderAnnotatedStrategy() throws Exception {

		mockMvc.perform(get("/profiles"))
				.andExpect(status().isNotFound());
	}

	/**
	 * Verifies that the ANNOTATED strategy correctly exposes the HTTP endpoint for {@link MemberRepository}: a GET to
	 * {@code /members} must return 200 OK.
	 */
	@Test // GH-1515
	void memberRepositoryIsExposedAsHttpEndpointUnderAnnotatedStrategy() throws Exception {

		mockMvc.perform(get("/members"))
				.andExpect(status().isOk());
	}
}
