/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.convert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * {@link Converter} to render all {@link Link}s contained in a {@link ResourceSupport} as {@code text/uri-list} and
 * parse a request of that media type back into a {@link ResourceSupport} instance.
 *
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
public class UriListHttpMessageConverter implements HttpMessageConverter<RepresentationModel<?>> {

	private static final List<MediaType> MEDIA_TYPES = new ArrayList<MediaType>();

	static {
		MEDIA_TYPES.add(MediaType.parseMediaType("text/uri-list"));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.HttpMessageConverter#canRead(java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {

		return null != mediaType //
				&& RepresentationModel.class.isAssignableFrom(clazz) //
				&& mediaType.getSubtype().contains("uri-list");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.HttpMessageConverter#canWrite(java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return canRead(clazz, mediaType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.HttpMessageConverter#getSupportedMediaTypes()
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return MEDIA_TYPES;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.HttpMessageConverter#read(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@Override
	public RepresentationModel<?> read(Class<? extends RepresentationModel<?>> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()))) {

			Links links = reader.lines() //
					.map(Link::of) //
					.reduce(Links.NONE, Links::and, Links::and);

			return CollectionModel.empty(links);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.HttpMessageConverter#write(java.lang.Object, org.springframework.http.MediaType, org.springframework.http.HttpOutputMessage)
	 */
	@Override
	public void write(RepresentationModel<?> resource, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));

		for (Link link : resource.getLinks()) {
			writer.write(link.getHref());
			writer.newLine();
		}

		writer.flush();
	}
}
