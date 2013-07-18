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
package org.springframework.data.rest.core.invoke;

import java.io.Serializable;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * A special {@link RepositoryInvoker} that shortcuts invocations to methods on {@link PagingAndSortingRepository} to
 * avoid reflection overhead introduced by the superclass.
 * 
 * @author Oliver Gierke
 */
class PagingAndSortingRepositoryInvoker extends CrudRepositoryInvoker {

	private final PagingAndSortingRepository<Object, Serializable> repository;

	/**
	 * Creates a new {@link PagingAndSortingRepositoryInvoker} using the given repository, {@link RepositoryInformation}
	 * and {@link ConversionService}.
	 * 
	 * @param repository must not be {@literal null}.
	 * @param information must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public PagingAndSortingRepositoryInvoker(PagingAndSortingRepository<Object, Serializable> repository,
			RepositoryInformation information, ConversionService conversionService) {

		super(repository, information, conversionService);
		this.repository = repository;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.CrudRepositoryInvoker#invokeFindAll(org.springframework.data.domain.Sort)
	 */
	@Override
	public Iterable<Object> invokeFindAll(Sort sort) {
		return sort == null ? invokeFindAll() : repository.findAll(sort);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.CrudRepositoryInvoker#invokeFindAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return pageable == null ? invokeFindAll() : repository.findAll(pageable);
	}
}
