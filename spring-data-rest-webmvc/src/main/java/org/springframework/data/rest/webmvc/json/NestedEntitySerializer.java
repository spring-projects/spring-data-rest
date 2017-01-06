/*
 * Copyright 2012-2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ResourceProcessorInvoker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Serializer to wrap values into an {@link Resource} instance and collecting all association links.
 *
 * @author Oliver Gierke
 * @author Anton Koscejev
 * @since 2.5
 */
public class NestedEntitySerializer extends StdSerializer<Object> {

	private static final long serialVersionUID = -2327469118972125954L;

	private final PersistentEntities entities;
	private final EmbeddedResourcesAssembler assembler;
	private final ResourceProcessorInvoker invoker;
	private final boolean unwrapping;
	private final NameTransformer unwrapper;

	public NestedEntitySerializer(PersistentEntities entities,
								  EmbeddedResourcesAssembler assembler,
								  ResourceProcessorInvoker invoker) {
		this(entities, assembler, invoker, false, null);
	}

	public NestedEntitySerializer(PersistentEntities entities,
								  EmbeddedResourcesAssembler assembler,
								  ResourceProcessorInvoker invoker,
								  boolean unwrapping, NameTransformer unwrapper) {

		super(Object.class);
		this.entities = entities;
		this.assembler = assembler;
		this.invoker = invoker;
		this.unwrapping = unwrapping;
		this.unwrapper = unwrapper;
	}

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		getResourceSerializer(provider)
				.serialize(toResources(value), gen, provider);
	}

	@Override
	public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
			throws IOException {
		getResourceSerializer(provider)
				.serializeWithType(toResources(value), gen, provider, typeSer);
	}

	private Object toResources(Object value) {

		if (value instanceof Collection) {

			Collection<?> source = (Collection<?>) value;
			List<Object> resources = new ArrayList<Object>();

			for (Object element : source) {
				resources.add(toResource(element));
			}

			return resources;
		} else {
			return toResource(value);
		}
	}

	private PersistentEntityResource toResource(Object value) {

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(value.getClass());

		PersistentEntityResource resource = PersistentEntityResource.build(value, entity)
				.withEmbedded(assembler.getEmbeddedResources(value))
				.buildNested();
		return invoker.invokeProcessorsFor(resource);
	}

	protected JsonSerializer<Object> getResourceSerializer(SerializerProvider serializers) throws JsonMappingException {

		JsonSerializer<Object> serializer = serializers.findValueSerializer(PersistentEntityResource.class, null);
		if (unwrapping) {
			return serializer.unwrappingSerializer(unwrapper);
		}
		return serializer;
	}

	@Override
	public boolean isUnwrappingSerializer() {
		return unwrapping;
	}

	@Override
	public JsonSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
		return new NestedEntitySerializer(entities, assembler, invoker, true, unwrapper);
	}
}
