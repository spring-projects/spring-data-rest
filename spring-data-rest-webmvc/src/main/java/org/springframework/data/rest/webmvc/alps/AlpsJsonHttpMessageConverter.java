/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.alps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.alps.Alps;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link HttpMessageConverter} to render {@link Alps} and {@link RootResourceInformation} instances as
 * {@code application/alps+json}.
 * 
 * @author Oliver Gierke
 */
public class AlpsJsonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

	private static final MediaType ALPS_MEDIA_TYPE = MediaType.parseMediaType("application/alps+json");

	private final RootResourceInformationToAlpsDescriptorConverter converter;

	/**
	 * Creates a new {@link AlpsJsonHttpMessageConverter} for the given {@link Converter}.
	 * 
	 * @param converter must not be {@literal null}.
	 */
	public AlpsJsonHttpMessageConverter(RootResourceInformationToAlpsDescriptorConverter converter) {

		Assert.notNull(converter, "Converter must not be null!");

		this.converter = converter;

		ObjectMapper mapper = getObjectMapper();
		mapper.setSerializationInclusion(Include.NON_EMPTY);

		setPrettyPrint(true);
		setSupportedMediaTypes(Arrays.asList(ALPS_MEDIA_TYPE, MediaType.APPLICATION_JSON, MediaType.ALL));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.MappingJackson2HttpMessageConverter#canWrite(java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return (clazz.isAssignableFrom(Alps.class) || clazz.isAssignableFrom(RootResourceInformation.class))
				&& super.canWrite(clazz, mediaType);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.MappingJackson2HttpMessageConverter#canRead(java.lang.reflect.Type, java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.MappingJackson2HttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	@Override
	protected void writeInternal(Object object, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {

		Object toWrite = object instanceof RootResourceInformation ? converter.convert((RootResourceInformation) object)
				: object;

		super.writeInternal(toWrite, outputMessage);
	}
}
