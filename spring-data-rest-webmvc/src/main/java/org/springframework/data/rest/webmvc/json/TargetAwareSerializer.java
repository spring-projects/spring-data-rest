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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.Resource;

/**
 * Serializer that will serialize projection wrapped into a {@link Resource} with links.
 * Projection will also be serialized with target's type information, unless it's overridden for projection.
 *
 * @author Oliver Gierke
 * @author Anton Koscejev
 */
@SuppressWarnings("serial")
class TargetAwareSerializer extends StdSerializer<TargetAware> {

	private final LinkCollector collector;
	private final Associations associations;
	private final boolean unwrapping;
	private final NameTransformer unwrapper;

	/**
	 * Creates a new {@link TargetAwareSerializer} for the given {@link LinkCollector} and {@link Associations}
	 * with unwrapping mode turned off.
	 *
	 * @param collector must not be {@literal null}.
	 * @param associations must not be {@literal null}.
	 */
	TargetAwareSerializer(LinkCollector collector, Associations associations) {
		this(collector, associations, false, null);
	}

	/**
	 * Creates a new {@link TargetAwareSerializer} for the given {@link LinkCollector}, {@link Associations}
	 * and whether to be in unwrapping mode or not.
	 *
	 * @param collector must not be {@literal null}.
	 * @param associations must not be {@literal null}.
	 * @param unwrapping true to create unwrapping serializer; false for default behavior
	 * @param unwrapper optional name transformer to use, if unwrapping
	 */
	TargetAwareSerializer(LinkCollector collector, Associations associations,
						  boolean unwrapping, NameTransformer unwrapper) {

		super(TargetAware.class);
		this.unwrapper = unwrapper;
		this.unwrapping = unwrapping;
		this.collector = collector;
		this.associations = associations;
	}

	private Resource<?> asResource(TargetAware projection) {

		if (projection instanceof Resource) {
			return (Resource<?>) projection;
		}
		Links links = associations.getMetadataFor(projection.getTargetClass()).isExported()
				? collector.getLinksFor(projection.getTarget()) : new Links();
		return TargetAwareResource.forProjection(projection, links);
	}

	@Override
	public TargetAwareSerializer unwrappingSerializer(NameTransformer unwrapper) {
		return new TargetAwareSerializer(collector, associations, true, unwrapper);
	}

	@Override
	public void serialize(TargetAware value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {

		Resource<?> rawValue = asResource(value);

		Class<?> projectionType = value.getClass().getInterfaces()[0];

		// allow customizing type information directly on projection class
		if (trySerializeWithType(value.getTarget(), rawValue, projectionType, gen, serializers)) {
			return;
		}

		// try to find type serializer for the projection source, i.e., proxy target
		if (trySerializeWithType(value.getTarget(), rawValue, value.getTargetClass(), gen, serializers)) {
			return;
		}

		getResourceSerializer(serializers)
				.serialize(rawValue, gen, serializers);
	}

	private boolean trySerializeWithType(Object target, Object resource, Class<?> typeInfo,
										 JsonGenerator gen, SerializerProvider serializers) throws IOException {

		JavaType targetType = serializers.getTypeFactory().constructType(typeInfo);
		TypeSerializer typeSerializer = serializers.findTypeSerializer(targetType);
		if (typeSerializer != null) {
			// content is serialized unwrapped, which has problems with type serialization,
			// so any type serialization must be applied on the resource level
			getResourceSerializer(serializers)
					.serializeWithType(resource, gen, serializers,
							new TargetAwareTypeSerializer(target, typeSerializer));
			return true;
		}
		return false;
	}

	@Override
	public void serializeWithType(TargetAware value, JsonGenerator gen,
								  SerializerProvider serializers, TypeSerializer typeSer)
			throws IOException {

		getResourceSerializer(serializers)
				.serializeWithType(asResource(value), gen, serializers, typeSer);
	}

	protected JsonSerializer<Object> getResourceSerializer(SerializerProvider serializers) throws JsonMappingException {

		JsonSerializer<Object> serializer = serializers.findValueSerializer(Resource.class, null);
		if (unwrapping) {
			return serializer.unwrappingSerializer(unwrapper);
		}
		return serializer;
	}

	@Override
	public boolean isUnwrappingSerializer() {
		return unwrapping;
	}
}
