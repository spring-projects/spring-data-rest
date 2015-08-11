/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Profile-based controller exposing multiple forms of metadata.
 *
 * @author Greg Turnquist
 * @see DATAREST-638
 * @since 2.4
 */
@BasePathAwareController
public class ProfileController {

	public static final String PROFILE_ROOT_MAPPING = "/profile";
	public static final String RESOURCE_PROFILE_MAPPING = PROFILE_ROOT_MAPPING + "/{repository}";

	private final RepositoryRestConfiguration configuration;
	private final RepositoryResourceMappings mappings;
	private final Repositories repositories;

	/**
	 * Wire up the controller with a copy of {@link RepositoryRestConfiguration}.
	 *
	 * @param configuration must not be {@literal null}.
	 * @param mappings must not be {@literal null}.
	 * @param repositories must not be {@literal null}.
	 */
	@Autowired
	public ProfileController(RepositoryRestConfiguration configuration, RepositoryResourceMappings mappings,
							 Repositories repositories) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");
		Assert.notNull(mappings, "RepositoryResourceMappings must not be null!");
		Assert.notNull(repositories, "Repositories must not be null!");

		this.configuration = configuration;
		this.mappings = mappings;
		this.repositories = repositories;
	}

	/**
	 * List the OPTIONS for this controller.
	 *
	 * @return
	 */
	@RequestMapping(value = PROFILE_ROOT_MAPPING, method = RequestMethod.OPTIONS)
	public HttpEntity<?> profileOptions() {

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(Collections.singleton(HttpMethod.GET));

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * List a profile link for each exported repository.
	 *
	 * @return
	 */
	@RequestMapping(value = PROFILE_ROOT_MAPPING, method = GET)
	HttpEntity<ResourceSupport> listAllFormsOfMetadata() {

		ResourceSupport profile = new ResourceSupport();

		profile.add(new Link(getRootPath(this.configuration)).withSelfRel());

		for (Class<?> domainType : this.repositories) {

			ResourceMetadata mapping = this.mappings.getMetadataFor(domainType);

			if (mapping.isExported()) {
				profile.add(new Link(getPath(this.configuration, mapping), mapping.getRel()));
			}
		}

		return new ResponseEntity<ResourceSupport>(profile, HttpStatus.OK);
	}

	/**
	 * Return href for the profile root link of a given baseUri.
	 *
	 * @param configuration is the source of the app's baseUri.
	 * @return
	 */
	public static String getRootPath(RepositoryRestConfiguration configuration) {

		BaseUri baseUri = new BaseUri(configuration.getBaseUri());
		return baseUri.getUriComponentsBuilder().path(ProfileController.PROFILE_ROOT_MAPPING).build().toString();
	}

	/**
	 * Return href for the profile link of a given baseUri and domain type mapping.
	 *
	 * @param configuration is the source of the app's baseUri.
	 * @param mapping provides the resource's path.
	 * @return
	 */
	public static String getPath(RepositoryRestConfiguration configuration, ResourceMapping mapping) {

		if (mapping == null) {
			return getRootPath(configuration);
		} else {
			return getRootPath(configuration) + mapping.getPath();
		}
	}
}
