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
package org.springframework.data.rest.webmvc.alps;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller exposing semantic documentation for the resources exposed using the Application Level Profile Semantics
 * format.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @see <a href="http://alps.io">http://alps.io</a>
 */
@BasePathAwareController
public class AlpsController {

	private final RepositoryRestConfiguration configuration;

	/**
	 * Creates a new {@link AlpsController} for the given {@link ResourceMappings}.
	 * 
	 * @param configuration must not be {@literal null}.
	 */
	@Autowired
	public AlpsController(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "MetadataConfiguration must not be null!");

		this.configuration = configuration;
	}

	/**
	 * Exposes the allowed HTTP methods for the ALPS resources.
	 * 
	 * @return
	 */
	@RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = OPTIONS,
			produces = RestMediaTypes.ALPS_JSON_VALUE)
	HttpEntity<?> alpsOptions() {

		verifyAlpsEnabled();

		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(Collections.singleton(HttpMethod.GET));

		return new ResponseEntity<Object>(headers, HttpStatus.OK);
	}

	/**
	 * Exposes an ALPS resource to describe an individual repository resource.
	 * 
	 * @param information
	 * @return
	 */
	@RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = GET,
			produces = { MediaType.ALL_VALUE, RestMediaTypes.ALPS_JSON_VALUE })
	HttpEntity<RootResourceInformation> descriptor(RootResourceInformation information) {

		verifyAlpsEnabled();

		return new ResponseEntity<RootResourceInformation>(information, HttpStatus.OK);
	}

	private void verifyAlpsEnabled() {

		if (!configuration.getMetadataConfiguration().alpsEnabled()) {
			throw new ResourceNotFoundException();
		}
	}
}
