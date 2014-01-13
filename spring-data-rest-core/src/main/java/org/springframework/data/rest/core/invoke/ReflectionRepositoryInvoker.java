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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.invoke.CrudVTable.CrudMethodDelegate;
import org.springframework.hateoas.core.AnnotationAttribute;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Base {@link RepositoryInvoker} using reflection to invoke methods on Spring Data Repositories.
 * 
 * @author Oliver Gierke
 */
class ReflectionRepositoryInvoker implements RepositoryInvoker {

	private static final AnnotationAttribute PARAM_ANNOTATION = new AnnotationAttribute(Param.class);

	private final Object repository;
	private final RepositoryInformation information;
	private final ConversionService conversionService;
	private final CrudVTable vTable;

	/**
	 * Creates a new {@link ReflectionRepositoryInvoker} for the given repository, {@link RepositoryInformation} and
	 * {@link ConversionService}.
	 * 
	 * @param repository must not be {@literal null}.
	 * @param information must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public ReflectionRepositoryInvoker(Object repository, RepositoryInformation information,
			ConversionService conversionService) {

		Assert.notNull(repository, "Repository must not be null!");
		Assert.notNull(information, "RepositoryInformation must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.vTable = new CrudVTable();
		this.repository = repository;
		this.information = information;
		this.conversionService = conversionService;
		
		CrudMethods methods = this.information.getCrudMethods();
		
		// Set up the 'vtable' to point to the appropriate repository CRUD methods.
		// While this notion is a little convoluted, it offers some advantage by way
		// of efficiency since some of the 'method choosing' reflection work can be 
		// done once here instead of being performed on a 'per-call' basis.
		
		if(methods.hasFindAllMethod() && exposes(methods.getFindAllMethod())) {
			// Set up the findAll methods. Note that the method delegates for both findAll(Sort)
			// and findAll(Pageable) will always point to the same repository method.
			// Note also that a single findAll method provides not only its level of capabilities but 
			// all of the lesser levels of capabilities as well. For example 'findAll(Pageable)' 
			// can also provide the equivalent functionality of 'findAll(Sort)' and 'findAll()'
			// by passing 'null' for the 'Sort' parameter.
			
			Method method = methods.getFindAllMethod();
			Class<?>[] types = method.getParameterTypes();
			CrudMethodDelegate standardMethodDelegate = new CrudMethodDelegate(repository, method);
			
			if (types.length == 0) {
				CrudMethodDelegate noArgDelegate =
						new CrudMethodDelegate(repository, method) {
									public <T> T invoke(Object... arguments) {
										return super.invoke(Collections.emptyList().toArray());
									};
						};
				vTable.setFindAllPagableMethod(noArgDelegate);
				vTable.setFindAllSortMethod(noArgDelegate);
			} else if (Sort.class.isAssignableFrom(types[0])) { 
				// If we have only a findAll method with a 'sort' parameter then use this
				// adaptor for handling 'pageable' invocations
				vTable.setFindAllPagableMethod(new CrudMethodDelegate(repository, method) {
					public <T> T invoke(Object... arguments) {
						Pageable pageable = (Pageable)arguments[0];
						return super.invoke(pageable == null ? null : pageable.getSort());
					}
				});
				vTable.setFindAllSortMethod(standardMethodDelegate);
			} else {
				// Just regular delegation
				vTable.setFindAllPagableMethod(standardMethodDelegate);
				vTable.setFindAllSortMethod(standardMethodDelegate);
			}
		}
		
		// Set up the 'save' method
		if(methods.hasSaveMethod() && exposes(methods.getSaveMethod()))
			vTable.setSaveMethod(new CrudMethodDelegate(repository, methods.getSaveMethod()));

		// Set up the 'findOne' method
		if(methods.hasFindOneMethod() && exposes(methods.getFindOneMethod()))
			vTable.setFindOneMethod(new CrudMethodDelegate(repository, methods.getFindOneMethod()));
		
		// Set up the 'delete' method
		if(methods.hasDelete() && exposes(methods.getDeleteMethod()))
			vTable.setDeleteMethod(new CrudMethodDelegate(repository, methods.getDeleteMethod()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesFindAll()
	 */
	@Override
	public boolean exposesFindAll() {
		return vTable.getFindAllPagableMethod() != null || vTable.getFindAllSortMethod() != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Sort)
	 */
	@Override
	public Iterable<Object> invokeFindAll(Sort sort) {
		return vTable.getFindAllSortMethod().invoke(sort);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeFindAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return vTable.getFindAllPagableMethod().invoke(pageable);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesSave()
	 */
	@Override
	public boolean exposesSave() {
		return vTable.getSaveMethod() != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeSave(java.lang.Object)
	 */
	@Override
	public Object invokeSave(Object object) {
		return vTable.getSaveMethod().invoke(object);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesFindOne()
	 */
	@Override
	public boolean exposesFindOne() {
		return vTable.getFindOneMethod() != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeFindOne(java.io.Serializable)
	 */
	@Override
	public Object invokeFindOne(Serializable id) {
		return vTable.getFindOneMethod().invoke(convertId(id));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvocationInformation#exposesDelete()
	 */
	@Override
	public boolean exposesDelete() {
		return vTable.getDeleteMethod() != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeDelete(java.io.Serializable)
	 */
	@Override
	public void invokeDelete(Serializable id) {
		vTable.getDeleteMethod().invoke(convertId(id));
	}
	
	private static boolean exposes(Method method) {
		RestResource annotation = AnnotationUtils.findAnnotation(method, RestResource.class);
		return annotation == null ? true : annotation.exported();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvoker#invokeQueryMethod(java.lang.reflect.Method, java.util.Map, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort)
	 */
	@Override
	public Object invokeQueryMethod(Method method, Map<String, String[]> parameters, Pageable pageable, Sort sort) {
		return ReflectionUtils.invokeMethod(method, repository, prepareParameters(method, parameters, pageable, sort));
	}

	private Object[] prepareParameters(Method method, Map<String, String[]> rawParameters, Pageable pageable, Sort sort) {

		List<MethodParameter> parameters = new MethodParameters(method, PARAM_ANNOTATION).getParameters();

		if (parameters.isEmpty()) {
			return new Object[0];
		}

		Object[] result = new Object[parameters.size()];
		Sort sortToUse = pageable == null ? sort : pageable.getSort();

		for (int i = 0; i < result.length; i++) {

			MethodParameter param = parameters.get(i);
			Class<?> targetType = param.getParameterType();

			if (Pageable.class.isAssignableFrom(targetType)) {
				result[i] = pageable;
			} else if (Sort.class.isAssignableFrom(targetType)) {
				result[i] = sortToUse;
			} else {

				String parameterName = param.getParameterName();

				if (!StringUtils.hasText(parameterName)) {
					throw new IllegalArgumentException("No @Param annotation found on query method " + method.getName()
							+ " for parameter " + parameterName);
				}

				String[] parameterValue = rawParameters.get(parameterName);
				Object value = parameterValue == null ? null : parameterValue.length == 1 ? parameterValue[0] : parameterValue;

				result[i] = conversionService.convert(value, TypeDescriptor.forObject(value), new TypeDescriptor(param));
			}
		}

		return result;
	}


	/**
	 * Converts the given id into the id type of the backing repository.
	 * 
	 * @param id must not be {@literal null}.
	 * @return
	 */
	protected Serializable convertId(Serializable id) {
		Assert.notNull(id, "Id must not be null!");
		return conversionService.convert(id, information.getIdType());
	}
}
