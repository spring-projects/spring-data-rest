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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReflectionEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

/**
 * @author Oliver Gierke
 */
public class RestClientRepositoryFactory extends RepositoryFactorySupport {

	private final RestOperations operations;
	private final ConversionService conversionService;

	/**
	 * @param operations must not be {@literal null}.
	 * @param conversionService can be {@literal null}.
	 */
	public RestClientRepositoryFactory(RestOperations operations, ConversionService conversionService) {

		Assert.notNull(operations, "RestOperations must not be null!");

		this.operations = operations;
		this.conversionService = conversionService == null ? new DefaultFormattingConversionService() : conversionService;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
	 */
	@Override
	public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return new ReflectionEntityInformation<T, ID>(domainClass);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Object getTargetRepository(RepositoryMetadata metadata) {
		return new Object();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return Repository.class;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new DefaultQueryLookupStrategy(operations, conversionService);
	}

	/**
	 * @author Oliver Gierke
	 */
	private static final class DefaultQueryLookupStrategy implements QueryLookupStrategy {

		private final RestOperations operations;
		private final ConversionService conversionService;

		/**
		 * @param operations must not be {@literal null}.
		 * @param conversionService must not be {@literal null}.
		 */
		public DefaultQueryLookupStrategy(RestOperations operations, ConversionService conversionService) {

			Assert.notNull(operations, "RestOperations must not be null!");
			Assert.notNull(conversionService, "ConversionService must not be null!");

			this.operations = operations;
			this.conversionService = conversionService;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.repository.core.NamedQueries)
		 */
		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

			Class<?> repositoryInterface = metadata.getRepositoryInterface();
			RestClient restClient = AnnotationUtils.findAnnotation(repositoryInterface, RestClient.class);

			RestClientConfig config = new RestClientConfig(restClient);

			Traverson traverson = new Traverson(config.getBaseUri(), config.getMediaTypes());
			traverson.setRestOperations(operations);

			return new TraversingRepositoryQuery(new TraversalQueryMethod(method, metadata), traverson, conversionService);
		}

		private static class RestClientConfig {

			private RestClient client;

			/**
			 * @param client
			 */
			public RestClientConfig(RestClient client) {
				this.client = client;
			}

			public URI getBaseUri() {

				for (String baseUri : Arrays.asList(client.baseUri(), client.value())) {

					if (StringUtils.hasText(baseUri)) {
						return URI.create(baseUri);
					}
				}

				throw new IllegalStateException("No base URI configured!");
			}

			public MediaType[] getMediaTypes() {

				if (client == null || client.mediaTypes().length == 0) {
					return new MediaType[] { MediaTypes.HAL_JSON };
				}

				MediaType[] result = new MediaType[client.mediaTypes().length];
				int i = 0;

				for (String type : client.mediaTypes()) {
					result[i++] = MediaType.parseMediaType(type);
				}

				return result;
			}
		}
	}
}
