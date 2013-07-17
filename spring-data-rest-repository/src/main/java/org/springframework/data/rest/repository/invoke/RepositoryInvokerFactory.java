/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.repository.invoke;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;

/**
 * @author Oliver Gierke
 */
public class RepositoryInvokerFactory {

	private final Repositories repositories;
	private final ConversionService conversionService;

	private final Map<Class<?>, RepositoryInvoker> invokers;

	/**
	 * @param repositories
	 */
	public RepositoryInvokerFactory(Repositories repositories, ConversionService conversionService) {

		this.repositories = repositories;
		this.conversionService = conversionService;
		this.invokers = new HashMap<Class<?>, RepositoryInvoker>();

	}

	@SuppressWarnings("unchecked")
	private RepositoryInvoker prepareInvokers(Class<?> domainType) {

		Object repository = repositories.getRepositoryFor(domainType);
		RepositoryInformation information = repositories.getRepositoryInformationFor(domainType);

		if (repository instanceof PagingAndSortingRepository) {
			return new PagingAndSortingRepositoryInvoker((PagingAndSortingRepository<Object, Serializable>) repository,
					information, conversionService);
		} else if (repository instanceof CrudRepository) {
			return new CrudRepositoryInvoker((CrudRepository<Object, Serializable>) repository, information,
					conversionService);
		} else {
			return new ReflectionRepositoryInvoker(repository, information, conversionService);
		}
	}

	public RepositoryInvoker getInvokerFor(Class<?> domainType) {

		RepositoryInvoker invoker = invokers.get(domainType);

		if (invoker != null) {
			return invoker;
		}

		invoker = prepareInvokers(domainType);
		invokers.put(domainType, invoker);

		return invoker;
	}
}
