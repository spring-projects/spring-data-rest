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
package org.springframework.data.rest.webmvc.support;

import java.io.Serializable;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.config.ResourceMetadataHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.data.rest.webmvc.util.UriUtils;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to resolve entity ids for injection int handler method arguments annotated with
 * {@link BackendId}.
 * 
 * @author Oliver Gierke
 */
public class BackendIdHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final PluginRegistry<BackendIdConverter, Class<?>> idConverters;
	private final ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;
	private final BaseUri baseUri;

	/**
	 * Creates a new {@link BackendIdHandlerMethodArgumentResolver} for the given {@link BackendIdConverter}s and
	 * {@link ResourceMetadataHandlerMethodArgumentResolver}.
	 * 
	 * @param idConverters the {@link BackendIdConverter}s registered in the system, must not be {@literal null}..
	 * @param resourceMetadataResolver the resolver to obtain {@link ResourceMetadata} from, must not be {@literal null}.
	 * @param baseUri must not be {@literal null}.
	 */
	public BackendIdHandlerMethodArgumentResolver(PluginRegistry<BackendIdConverter, Class<?>> idConverters,
			ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver, BaseUri baseUri) {

		Assert.notNull(idConverters, "Id converters must not be null!");
		Assert.notNull(resourceMetadataResolver, "ResourceMetadata resolver must not be null!");
		Assert.notNull(baseUri, "BaseUri must not be null!");

		this.idConverters = idConverters;
		this.resourceMetadataResolver = resourceMetadataResolver;
		this.baseUri = baseUri;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(BackendId.class);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Serializable resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		Class<?> parameterType = parameter.getParameterType();

		if (parameter.getMethodAnnotation(BackendId.class) != null && !parameterType.equals(Serializable.class)) {
			throw new IllegalArgumentException(String.format(
					"Method parameter for @%s must be of type %s! Got %s for method %s.", BackendId.class.getSimpleName(),
					Serializable.class.getSimpleName(), parameterType.getSimpleName(), parameter.getMethod()));
		}

		ResourceMetadata metadata = resourceMetadataResolver.resolveArgument(parameter, mavContainer, request,
				binderFactory);

		if (metadata == null) {
			throw new IllegalArgumentException("Could not obtain ResourceMetadata for request " + request);
		}

		BackendIdConverter pluginFor = idConverters.getPluginFor(metadata.getDomainType(), DefaultIdConverter.INSTANCE);
		String lookupPath = baseUri.getRepositoryLookupPath(request);
		return pluginFor.fromRequestId(UriUtils.findMappingVariable("id", parameter.getMethod(), lookupPath),
				metadata.getDomainType());
	}
}
