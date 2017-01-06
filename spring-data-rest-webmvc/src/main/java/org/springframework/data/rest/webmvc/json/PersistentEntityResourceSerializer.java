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

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.Resource;

/**
 * Custom {@link JsonSerializer} for {@link PersistentEntityResource}s to turn associations into {@link Link}s.
 * Delegates to standard {@link Resource} serialization afterwards.
 *
 * @author Oliver Gierke
 * @author Anton Koscejev
 */
@SuppressWarnings("serial")
class PersistentEntityResourceSerializer extends StdSerializer<PersistentEntityResource> {

	private static final Logger LOG = LoggerFactory.getLogger(PersistentEntityResourceSerializer.class);

	private final LinkCollector collector;

	/**
	 * Creates a new {@link PersistentEntityResourceSerializer} using the given {@link LinkCollector}.
	 *
	 * @param collector must not be {@literal null}.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	PersistentEntityResourceSerializer(LinkCollector collector) {

		super(PersistentEntityResource.class);

		this.collector = collector;
	}

	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(
	 *  java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(final PersistentEntityResource resource, JsonGenerator jgen, SerializerProvider provider)
			throws IOException {

		serializeWithTypeOptional(resource, jgen, provider, null);
	}

	@Override
	public void serializeWithType(PersistentEntityResource resource, JsonGenerator jgen,
								  SerializerProvider provider, TypeSerializer typeSer) throws IOException {

		serializeWithTypeOptional(resource, jgen, provider, typeSer);
	}

	private void serializeWithTypeOptional(final PersistentEntityResource resource, JsonGenerator jgen,
										   SerializerProvider provider, TypeSerializer typeSerializer)
			throws IOException {

		LOG.debug("Serializing PersistentEntity {}.", resource.getPersistentEntity());

		Object content = resource.getContent();

		if (hasScalarSerializer(content, provider)) {
			provider.defaultSerializeValue(content, jgen);
			return;
		}

		Links links = getLinks(resource);
		Resource<?> resourceToRender;
		JavaType targetType;

		if (content instanceof TargetAware) {
			TargetAware projection = (TargetAware) content;
			resourceToRender = TargetAwareResource.forProjection(projection, links);
			// resource should infer type from the projection
			if (typeSerializer == null) {
				Class<?> projectionType = projection.getClass().getInterfaces()[0];
				targetType = provider.getTypeFactory().constructType(projectionType);
				typeSerializer = provider.findTypeSerializer(targetType);
			}
			// if projection has no type serializer, try falling back to the target's type
			if (typeSerializer == null) {
				targetType = provider.getTypeFactory().constructType(projection.getTargetClass());
				typeSerializer = provider.findTypeSerializer(targetType);
				content = projection.getTarget();
			}
		} else {
			resourceToRender = new TargetAwareResource<Object>(content, content, content.getClass(), links) {
				@JsonUnwrapped
				public Iterable<?> getEmbedded() {
					return resource.getEmbeddeds();
				}
			};
			if (typeSerializer == null) {
				targetType = provider.getTypeFactory().constructType(content.getClass());
				typeSerializer = provider.findTypeSerializer(targetType);
			}
		}

		if (typeSerializer != null) {
			// serialize with custom type-serializer that will use the of content, not resource
			provider.findValueSerializer(Resource.class)
					.serializeWithType(resourceToRender, jgen, provider,
							new TargetAwareTypeSerializer(content, typeSerializer));
		} else {
			provider.findValueSerializer(Resource.class)
					.serialize(resourceToRender, jgen, provider);
		}
	}

	private Links getLinks(PersistentEntityResource resource) {

		Object source = getLinkSource(resource.getContent());
		return resource.isNested() ? collector.getLinksForNested(source, resource.getLinks())
				: collector.getLinksFor(source, resource.getLinks());
	}

	private Object getLinkSource(Object object) {
		return TargetAware.class.isInstance(object) ? ((TargetAware) object).getTarget() : object;
	}

	private static boolean hasScalarSerializer(Object source, SerializerProvider provider) throws
			JsonMappingException {

		JsonSerializer<Object> serializer = provider.findValueSerializer(source.getClass());
		return serializer instanceof ToStringSerializer || serializer instanceof StdScalarSerializer;
	}
}
