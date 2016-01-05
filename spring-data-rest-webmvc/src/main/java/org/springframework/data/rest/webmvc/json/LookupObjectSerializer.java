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
import java.util.Collection;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.plugin.core.PluginRegistry;

@RequiredArgsConstructor
public class LookupObjectSerializer extends ToStringSerializer {

	private static final long serialVersionUID = -3033458643050330913L;
	private final PluginRegistry<EntityLookup<?>, Class<?>> lookups;

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {

		if (value instanceof Collection) {

			gen.writeStartArray();

			for (Object element : (Collection<?>) value) {
				gen.writeString(getLookupKey(element));
			}

			gen.writeEndArray();
		} else {
			gen.writeString(getLookupKey(value));
		}
	}

	@SuppressWarnings("unchecked")
	private String getLookupKey(Object value) {

		EntityLookup<Object> lookup = (EntityLookup<Object>) lookups.getPluginFor(value.getClass());
		return lookup.getResourceIdentifier(value).toString();
	}
}
