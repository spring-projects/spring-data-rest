/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import java.io.InputStream;

import org.springframework.data.rest.webmvc.IncomingRequest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter;
import org.springframework.data.rest.webmvc.json.patch.Patch;
import org.springframework.data.rest.webmvc.util.InputStreamHttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Component to apply JSON Patch and JSON Merge Patch payloads to existing domain objects. The implementation uses the
 * JSON Patch library by Francis Galiegue but applies a customization to remove operations. As the patched node set
 * needs to be applied to the existing domain objects (see {@link DomainObjectReader} for that) we turn remove calls
 * into
 *
 * @author Oliver Gierke
 * @author Mathias Düsterhöft
 * @see <a href="https://tools.ietf.org/html/rfc6902">https://tools.ietf.org/html/rfc6902</a>
 * @see <a href=
 *      "https://tools.ietf.org/html/draft-ietf-appsawg-json-merge-patch-02">https://tools.ietf.org/html/draft-ietf-appsawg-json-merge-patch-02</a>
 */
class JsonPatchHandler {

	private final ObjectMapper mapper;
	private final ObjectMapper sourceMapper;
	private final DomainObjectReader reader;

	/**
	 * Creates a new {@link JsonPatchHandler} with the given {@link ObjectMapper} and {@link DomainObjectReader}.
	 *
	 * @param mapper must not be {@literal null}.
	 * @param reader must not be {@literal null}.
	 */
	public JsonPatchHandler(ObjectMapper mapper, DomainObjectReader reader) {

		Assert.notNull(mapper, "ObjectMapper must not be null!");
		Assert.notNull(reader, "DomainObjectReader must not be null!");

		this.mapper = mapper;
		this.reader = reader;

		this.sourceMapper = mapper.copy();
		this.sourceMapper.setSerializationInclusion(Include.NON_NULL);
	}

	/**
	 * Applies the body of the given {@link IncomingRequest} as patch on the given target object.
	 *
	 * @param request must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @return
	 * @throws Exception
	 */
	public <T> T apply(IncomingRequest request, T target) throws Exception {

		Assert.notNull(request, "Request must not be null!");
		Assert.isTrue(request.isPatchRequest(), "Cannot handle non-PATCH request!");
		Assert.notNull(target, "Target must not be null!");

		if (request.isJsonPatchRequest()) {
			return applyPatch(request.getBody(), target);
		} else {
			return applyMergePatch(request.getBody(), target);
		}
	}

	@SuppressWarnings("unchecked")
	<T> T applyPatch(InputStream source, T target) throws Exception {
		return getPatchOperations(source).apply(target, (Class<T>) target.getClass());
	}

	<T> T applyMergePatch(InputStream source, T existingObject) throws Exception {
		return reader.read(source, existingObject, mapper);
	}

	<T> T applyPut(ObjectNode source, T existingObject) throws Exception {
		return reader.readPut(source, existingObject, mapper);
	}

	/**
	 * Returns all {@link JsonPatchOperation}s to be applied.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 * @throws HttpMessageNotReadableException in case the payload can't be read.
	 */
	private Patch getPatchOperations(InputStream source) {

		try {
			return new JsonPatchPatchConverter(mapper).convert(mapper.readTree(source));
		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException(
					String.format("Could not read PATCH operations! Expected %s!", RestMediaTypes.JSON_PATCH_JSON), o_O,
					InputStreamHttpInputMessage.of(source));
		}
	}
}
