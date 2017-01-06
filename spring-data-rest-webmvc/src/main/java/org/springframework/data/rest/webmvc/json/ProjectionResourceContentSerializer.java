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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Serializer that will delegate serializing projection to
 * serializer for {@link ProjectionResourceContent#getProjectionType() projection type}.
 *
 * @author Oliver Gierke
 * @author Anton Koscejev
 */
@SuppressWarnings("serial")
class ProjectionResourceContentSerializer extends StdSerializer<ProjectionResourceContent> {

	private final boolean unwrapping;
	private final NameTransformer unwrapper;

	ProjectionResourceContentSerializer() {
		this(false, null);
	}

	ProjectionResourceContentSerializer(boolean unwrapping, NameTransformer unwrapper) {

		super(ProjectionResourceContent.class);
		this.unwrapping = unwrapping;
		this.unwrapper = unwrapper;
	}

	@Override
	public void serialize(ProjectionResourceContent value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {

		getContentSerializer(value, serializers)
				.serialize(value.getProjection(), gen, serializers);
	}

	@Override
	public void serializeWithType(ProjectionResourceContent value, JsonGenerator gen,
								  SerializerProvider serializers, TypeSerializer typeSer)
			throws IOException {

		getContentSerializer(value, serializers)
				.serializeWithType(value.getProjection(), gen, serializers, typeSer);
	}

	private JsonSerializer<Object> getContentSerializer(
			ProjectionResourceContent value, SerializerProvider serializers) throws JsonMappingException {

		JsonSerializer<Object> serializer = serializers.findValueSerializer(value.getProjectionType(), null);
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
	public JsonSerializer<ProjectionResourceContent> unwrappingSerializer(NameTransformer unwrapper) {
		return new ProjectionResourceContentSerializer(true, unwrapper);
	}
}
