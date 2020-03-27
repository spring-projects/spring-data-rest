/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.Collections;
import java.util.Optional;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class ControllerUtils {

	/**
	 * Use {@link CollectionModel#empty()} instead.
	 *
	 * @deprecated since 3.3
	 */
	@Deprecated public static final Iterable<EntityModel<?>> EMPTY_RESOURCE_LIST = Collections.emptyList();

	public static <R extends RepresentationModel<?>> ResponseEntity<RepresentationModel<?>> toResponseEntity(
			HttpStatus status, HttpHeaders headers, Optional<R> resource) {

		HttpHeaders hdrs = new HttpHeaders();

		if (headers != null) {
			hdrs.putAll(headers);
		}

		return new ResponseEntity<RepresentationModel<?>>(resource.orElse(null), hdrs, status);
	}

	/**
	 * Wrap a resource as a {@link ResourceEntity} and attach given headers and status.
	 *
	 * @param status
	 * @param headers
	 * @param resource
	 * @param <R>
	 * @return
	 */
	public static <R extends RepresentationModel<?>> ResponseEntity<RepresentationModel<?>> toResponseEntity(
			HttpStatus status, HttpHeaders headers, R resource) {

		Assert.notNull(status, "Http status must not be null!");
		Assert.notNull(headers, "Http headers must not be null!");
		Assert.notNull(resource, "Payload must not be null!");

		return toResponseEntity(status, headers, Optional.of(resource));
	}

	/**
	 * Return an empty response that is only comprised of a status
	 *
	 * @param status
	 * @return
	 */
	public static ResponseEntity<RepresentationModel<?>> toEmptyResponse(HttpStatus status) {
		return toEmptyResponse(status, new HttpHeaders());
	}

	/**
	 * Return an empty response that is only comprised of headers and a status
	 *
	 * @param status
	 * @param headers
	 * @return
	 */
	public static ResponseEntity<RepresentationModel<?>> toEmptyResponse(HttpStatus status, HttpHeaders headers) {
		return toResponseEntity(status, headers, Optional.empty());
	}
}
