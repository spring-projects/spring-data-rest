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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * TypeSerializer that delegates to another TypeSerializer,
 * with the exception of applying type of the specified custom value (e.g., projection target),
 * not the actual value being serialized (e.g., projection resource).
 *
 * @author Anton Koscejev
 */
class TargetAwareTypeSerializer extends TypeSerializer {

	private final TypeSerializer delegate;
	private final Object valueOverride;

	TargetAwareTypeSerializer(Object valueOverride, TypeSerializer delegate) {

		this.valueOverride = valueOverride;
		this.delegate = delegate;
	}

	@Override
	public TypeSerializer forProperty(BeanProperty prop) {
		return new TargetAwareTypeSerializer(valueOverride, delegate.forProperty(prop));
	}

	@Override
	public JsonTypeInfo.As getTypeInclusion() {
		return delegate.getTypeInclusion();
	}

	@Override
	public String getPropertyName() {
		return delegate.getPropertyName();
	}

	@Override
	public TypeIdResolver getTypeIdResolver() {
		return delegate.getTypeIdResolver();
	}

	@Override
	public void writeTypePrefixForScalar(Object value, JsonGenerator jgen) throws IOException {
		delegate.writeTypePrefixForScalar(valueOverride, jgen);
	}

	@Override
	public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException {
		delegate.writeTypePrefixForObject(valueOverride, jgen);
	}

	@Override
	public void writeTypePrefixForArray(Object value, JsonGenerator jgen) throws IOException {
		delegate.writeTypePrefixForArray(valueOverride, jgen);
	}

	@Override
	public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen) throws IOException {
		delegate.writeTypeSuffixForScalar(valueOverride, jgen);
	}

	@Override
	public void writeTypeSuffixForObject(Object value, JsonGenerator jgen) throws IOException {
		delegate.writeTypeSuffixForObject(valueOverride, jgen);
	}

	@Override
	public void writeTypeSuffixForArray(Object value, JsonGenerator jgen) throws IOException {
		delegate.writeTypeSuffixForArray(valueOverride, jgen);
	}

	@Override
	public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
		delegate.writeTypePrefixForScalar(valueOverride, jgen, type);
	}

	@Override
	public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
		delegate.writeTypePrefixForObject(valueOverride, jgen, type);
	}

	@Override
	public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
		delegate.writeTypePrefixForArray(valueOverride, jgen, type);
	}

	@Override
	public void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException {
		delegate.writeCustomTypePrefixForScalar(valueOverride, jgen, typeId);
	}

	@Override
	public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException {
		delegate.writeCustomTypePrefixForObject(valueOverride, jgen, typeId);
	}

	@Override
	public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException {
		delegate.writeCustomTypePrefixForArray(valueOverride, jgen, typeId);
	}

	@Override
	public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException {
		delegate.writeCustomTypeSuffixForScalar(valueOverride, jgen, typeId);
	}

	@Override
	public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException {
		delegate.writeCustomTypeSuffixForObject(valueOverride, jgen, typeId);
	}

	@Override
	public void writeCustomTypeSuffixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException {
		delegate.writeCustomTypeSuffixForArray(valueOverride, jgen, typeId);
	}
}
