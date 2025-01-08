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
package org.springframework.data.rest.webmvc;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.JsonSchema;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to expose a JSON schema via {@code /repository/schema}.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @see <a href="https://json-schema.org/">https://json-schema.org/</a>
 */
@BasePathAwareController
class RepositorySchemaController {

	private final PersistentEntityToJsonSchemaConverter jsonSchemaConverter;

	/**
	 * Creates a new {@link RepositorySchemaController} using the given {@link PersistentEntityToJsonSchemaConverter}.
	 *
	 * @param jsonSchemaConverter must not be {@literal null}.
	 */
	@Autowired
	public RepositorySchemaController(PersistentEntityToJsonSchemaConverter jsonSchemaConverter) {

		Assert.notNull(jsonSchemaConverter, "PersistentEntityToJsonSchemaConverter must not be null");

		this.jsonSchemaConverter = jsonSchemaConverter;
	}

	/**
	 * Exposes a JSON schema for the repository referenced.
	 *
	 * @param resourceInformation will never be {@literal null}.
	 * @return
	 */
	@RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = GET,
			produces = RestMediaTypes.SCHEMA_JSON_VALUE)
	public HttpEntity<JsonSchema> schema(RootResourceInformation resourceInformation) {

		JsonSchema schema = jsonSchemaConverter.convert(resourceInformation.getDomainType());
		return new ResponseEntity<JsonSchema>(schema, HttpStatus.OK);
	}
}
