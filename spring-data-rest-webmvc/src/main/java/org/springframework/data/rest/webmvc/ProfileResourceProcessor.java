/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.util.Assert;

/**
 * {@link ResourceProcessor} to add a {@code profile} link to the root resource to point to multiple forms of metadata.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @since 2.4
 */
public class ProfileResourceProcessor implements ResourceProcessor<RepositoryLinksResource> {

	public static final String PROFILE_REL = "profile";

	private final RepositoryRestConfiguration configuration;

	/**
	 * Creates a new {@link ProfileResourceProcessor} with the given {@link RepositoryRestConfiguration}.
	 *
	 * @param configuration must not be {@literal null}.
	 */
	@Autowired
	public ProfileResourceProcessor(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");
		this.configuration = configuration;
	}

	/**
	 * Add a link to the {@link ProfileController}'s base URI to the app's root URI.
	 *
	 * @param resource
	 * @return
	 */
	@Override
	public RepositoryLinksResource process(RepositoryLinksResource resource) {

		resource.add(new Link(ProfileController.getRootPath(this.configuration), PROFILE_REL));

		return resource;
	}
}
