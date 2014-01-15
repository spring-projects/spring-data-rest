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
package org.springframework.data.rest.webmvc.convert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * @author Jon Brisbin
 * @author Greg Turnquist
 */
public class UriListHttpMessageConverter implements HttpMessageConverter<ResourceSupport> {

	private static final List<MediaType> MEDIA_TYPES = new ArrayList<MediaType>();

	static {
		MEDIA_TYPES.add(MediaType.parseMediaType("text/uri-list"));
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (null == mediaType) {
			return false;
		}
		return ResourceSupport.class.isAssignableFrom(clazz) && mediaType.getSubtype().contains("uri-list");
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return canRead(clazz, mediaType);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return MEDIA_TYPES;
	}

	@Override
	public ResourceSupport read(Class<? extends ResourceSupport> clazz, HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		List<Link> links = new ArrayList<Link>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()));
		String line;
		while (null != (line = reader.readLine())) {
			links.add(new Link(line));
		}
		return new Resources<Object>(Collections.emptyList(), links);
	}

	@Override
	public void write(ResourceSupport resource, MediaType contentType, HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));
		for (Link link : resource.getLinks()) {
			writer.write(link.getHref());
			writer.newLine();
		}
		writer.flush();
	}

}
