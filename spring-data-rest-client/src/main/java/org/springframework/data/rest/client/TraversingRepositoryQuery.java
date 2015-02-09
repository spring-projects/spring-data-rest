/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.client.Traverson.TraversalBuilder;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class TraversingRepositoryQuery implements RepositoryQuery {

	private final TraversalQueryMethod method;
	private final Traverson traverson;
	private final ConversionService conversionService;

	public TraversingRepositoryQuery(TraversalQueryMethod method, Traverson traverson, ConversionService conversionService) {

		this.method = method;
		this.traverson = traverson;
		this.conversionService = conversionService;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	@Override
	public Object execute(Object[] parameters) {

		ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), parameters);
		Map<String, Object> traversalParameters = new HashMap<String, Object>();

		for (Parameter parameter : method.getParameters()) {

			String name = parameter.getName();

			if (StringUtils.hasText(name)) {
				traversalParameters.put(name,
						conversionService.convert(accessor.getBindableValue(parameter.getIndex()), String.class));
			}
		}

		TraversalBuilder builder = traverson.follow(method.getTraversal().rels()).withTemplateParameters(
				traversalParameters);

		if (method.isCollectionQuery()) {

			ParameterizedTypeReference<Resources<?>> reference = getTypeReference(method);
			return builder.toObject(reference).getContent();

		} else if (method.isQueryForEntity()) {

			ParameterizedTypeReference<Resource<?>> reference = getTypeReference(method);
			return builder.toObject(reference).getContent();
		}

		throw new IllegalStateException();
	}

	@SuppressWarnings("unchecked")
	private static <T> ParameterizedTypeReference<T> getTypeReference(TraversalQueryMethod method) {

		Class<?> wrapperType = method.isCollectionQuery() ? Resources.class : Resource.class;
		Class<?> objectType = method.getReturnedObjectType();

		return (ParameterizedTypeReference<T>) new CustomTypeReference(ResolvableType.forClassWithGenerics(wrapperType,
				objectType));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
	 */
	@Override
	public QueryMethod getQueryMethod() {
		return method;
	}

	static class CustomTypeReference extends ParameterizedTypeReference<Object> {

		private final ResolvableType type;

		/**
		 * @param type
		 */
		public CustomTypeReference(ResolvableType type) {
			this.type = type;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.ParameterizedTypeReference#getType()
		 */
		@Override
		public Type getType() {
			return type.hasGenerics() ? new TypeStub(type) : type.getType();
		}
	}

	static class TypeStub implements ParameterizedType {

		private final ResolvableType type;

		/**
		 * @param type
		 */
		public TypeStub(ResolvableType type) {
			this.type = type;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getActualTypeArguments()
		 */
		@Override
		public Type[] getActualTypeArguments() {

			ResolvableType[] generics = type.getGenerics();
			Type[] result = new Type[generics.length];

			for (int i = 0; i < generics.length; i++) {
				result[i] = type.resolveGeneric(i);
			}

			return result;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getRawType()
		 */
		@Override
		public Type getRawType() {
			return type.getRawClass();
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getOwnerType()
		 */
		@Override
		public Type getOwnerType() {
			return null;
		}
	}
}
