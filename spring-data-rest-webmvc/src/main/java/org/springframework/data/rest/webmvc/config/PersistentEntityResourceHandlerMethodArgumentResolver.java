/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.webmvc.IncomingRequest;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.support.BackendIdHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Custom {@link HandlerMethodArgumentResolver} to create {@link PersistentEntityResource} instances.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class PersistentEntityResourceHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String ERROR_MESSAGE = "Could not read an object of type %s from the request!";
	private static final String NO_CONVERTER_FOUND = "No suitable HttpMessageConverter found to read request body into object of type %s from request with content type of %s!";

	private final RootResourceInformationHandlerMethodArgumentResolver resourceInformationResolver;

	private final BackendIdHandlerMethodArgumentResolver idResolver;
	private final DomainObjectReader reader;
	private final List<HttpMessageConverter<?>> messageConverters;

	/**
	 * Creates a new {@link PersistentEntityResourceHandlerMethodArgumentResolver} for the given
	 * {@link HttpMessageConverter}s and {@link RootResourceInformationHandlerMethodArgumentResolver}..
	 * 
	 * @param messageConverters must not be {@literal null}.
	 * @param resourceInformationResolver must not be {@literal null}.
	 * @param idResolver must not be {@literal null}.
	 * @param reader must not be {@literal null}.
	 */
	public PersistentEntityResourceHandlerMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
			RootResourceInformationHandlerMethodArgumentResolver resourceInformationResolver,
			BackendIdHandlerMethodArgumentResolver idResolver, DomainObjectReader reader) {

		Assert.notEmpty(messageConverters, "MessageConverters must not be null or empty!");
		Assert.notNull(resourceInformationResolver,
				"RootResourceInformationHandlerMethodArgumentResolver must not be empty!");
		Assert.notNull(idResolver, "BackendIdHandlerMethodArgumentResolver must not be null!");
		Assert.notNull(reader, "DomainObjectReader must not be null!");

		this.messageConverters = messageConverters;
		this.resourceInformationResolver = resourceInformationResolver;
		this.idResolver = idResolver;
		this.reader = reader;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PersistentEntityResource.class.isAssignableFrom(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		RootResourceInformation resourceInformation = resourceInformationResolver.resolveArgument(parameter, mavContainer,
				webRequest, binderFactory);

		HttpServletRequest nativeRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		ServletServerHttpRequest request = new ServletServerHttpRequest(nativeRequest);
		IncomingRequest incoming = new IncomingRequest(request);

		Class<?> domainType = resourceInformation.getDomainType();
		MediaType contentType = request.getHeaders().getContentType();

		for (HttpMessageConverter converter : messageConverters) {

			if (!converter.canRead(PersistentEntityResource.class, contentType)) {
				continue;
			}

			Serializable id = idResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
			Object obj = read(resourceInformation, incoming, converter, id);

			if (obj == null) {
				throw new HttpMessageNotReadableException(String.format(ERROR_MESSAGE, domainType));
			}

			return PersistentEntityResource.build(obj, resourceInformation.getPersistentEntity()).build();
		}

		throw new HttpMessageNotReadableException(String.format(NO_CONVERTER_FOUND, domainType, contentType));
	}

	/**
	 * Reads the given {@link ServerHttpRequest} into an object of the type of the given {@link RootResourceInformation},
	 * potentially applying the content to an object of the given id.
	 * 
	 * @param information must not be {@literal null}.
	 * @param request must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @param id must not be {@literal null}.
	 * @return
	 */
	private Object read(RootResourceInformation information, IncomingRequest request,
			HttpMessageConverter<Object> converter, Serializable id) {

		Object objectToUpdate = getObjectToUpdate(id, information);

		// JSON + PATCH request
		if (request.isPatchRequest() && converter instanceof MappingJackson2HttpMessageConverter) {

			if (objectToUpdate == null) {
				new ResourceNotFoundException();
			}

			ObjectMapper mapper = ((MappingJackson2HttpMessageConverter) converter).getObjectMapper();
			Object result = readPatch(request, mapper, objectToUpdate);

			return result;

			// JSON + PUT request
		} else if (converter instanceof MappingJackson2HttpMessageConverter) {

			ObjectMapper mapper = ((MappingJackson2HttpMessageConverter) converter).getObjectMapper();

			return objectToUpdate == null ? read(request, converter, information) : readPutForUpdate(request, mapper,
					objectToUpdate);
		}

		// Catch all
		return read(request, converter, information);
	}

	private Object readPatch(IncomingRequest request, ObjectMapper mapper, Object existingObject) {

		try {
			JsonPatchHandler handler = new JsonPatchHandler(mapper, reader);
			return handler.apply(request, existingObject);
		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException(String.format(ERROR_MESSAGE, existingObject.getClass()), o_O);
		}
	}

	private Object readPutForUpdate(IncomingRequest request, ObjectMapper mapper, Object existingObject) {

		try {

			JsonPatchHandler handler = new JsonPatchHandler(mapper, reader);
			JsonNode jsonNode = mapper.readTree(request.getBody());

			return handler.applyPut((ObjectNode) jsonNode, existingObject);

		} catch (Exception o_O) {
			throw new HttpMessageNotReadableException(String.format(ERROR_MESSAGE, existingObject.getClass()), o_O);
		}
	}

	private Object read(IncomingRequest request, HttpMessageConverter<Object> converter,
			RootResourceInformation information) {

		try {
			return converter.read(information.getDomainType(), request.getServerHttpRequest());
		} catch (IOException o_O) {
			throw new HttpMessageNotReadableException(String.format(ERROR_MESSAGE, information.getDomainType()), o_O);
		}
	}

	/**
	 * Returns the object to be updated identified by the given id using the given {@link RootResourceInformation}.
	 * 
	 * @param id can be {@literal null}.
	 * @param information must not be {@literal null}.
	 * @return
	 */
	private static Object getObjectToUpdate(Serializable id, RootResourceInformation information) {

		if (id == null) {
			return null;
		}

		RepositoryInvoker invoker = information.getInvoker();
		return invoker.invokeFindOne(id);
	}
}
