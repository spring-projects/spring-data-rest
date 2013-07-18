/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.rest.core.support;

import static org.springframework.data.rest.core.support.ResourceStringUtils.*;
import static org.springframework.core.annotation.AnnotationUtils.*;
import static org.springframework.util.StringUtils.*;

import java.lang.reflect.Method;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.config.ResourceMapping;

/**
 * Helper methods to get the default rel and path values or to use values supplied by annotations.
 * 
 * @author Jon Brisbin
 * @author Florent Biville
 * @author Oliver Gierke
 */
@Deprecated
public abstract class ResourceMappingUtils {

	protected ResourceMappingUtils() {}

	public static String findRel(Class<?> type) {

		RestResource anno = findAnnotation(type, RestResource.class);
		if (anno != null) {
			if (hasText(anno.rel())) {
				return anno.rel();
			}
		}

		return uncapitalize(type.getSimpleName().replaceAll("Repository", ""));
	}

	public static String findRel(Method method) {

		RestResource anno = findAnnotation(method, RestResource.class);

		if (anno != null) {
			if (hasText(anno.rel())) {
				return anno.rel();
			}
		}

		return method.getName();
	}

	public static String formatRel(RepositoryRestConfiguration config, RepositoryInformation repoInfo,
			PersistentProperty<?> persistentProperty) {

		if (persistentProperty == null) {
			return null;
		}

		ResourceMapping repoMapping = getResourceMapping(config, repoInfo);
		ResourceMapping entityMapping = getResourceMapping(config, persistentProperty.getOwner());
		ResourceMapping propertyMapping = entityMapping.getResourceMappingFor(persistentProperty.getName());

		return String.format("%s.%s.%s", repoMapping.getRel(), entityMapping.getRel(),
				(null != propertyMapping ? propertyMapping.getRel() : persistentProperty.getName()));
	}

	public static String findPath(Class<?> type) {

		RestResource anno = findAnnotation(type, RestResource.class);

		if (anno != null) {
			if (hasTextExceptSlash(anno.path())) {
				return removeLeadingSlash(anno.path());
			}
		}

		return uncapitalize(type.getSimpleName().replaceAll("Repository", ""));
	}

	public static String findPath(Method method) {

		RestResource anno = findAnnotation(method, RestResource.class);

		if (anno != null) {
			if (hasTextExceptSlash(anno.path())) {
				return removeLeadingSlash(anno.path());
			}
		}

		return method.getName();
	}

	public static boolean findExported(Class<?> type) {
		RestResource anno = findAnnotation(type, RestResource.class);
		return anno == null || anno.exported();
	}

	public static boolean findExported(Method method) {
		RestResource anno = findAnnotation(method, RestResource.class);
		return anno == null || anno.exported();
	}

	public static ResourceMapping getResourceMapping(RepositoryRestConfiguration config, RepositoryInformation repoInfo) {
		if (null == repoInfo) {
			return null;
		}
		Class<?> repoType = repoInfo.getRepositoryInterface();
		ResourceMapping mapping = (null != config ? config.getResourceMappingForRepository(repoType) : null);
		return merge(repoType, mapping);
	}

	public static ResourceMapping getResourceMapping(RepositoryRestConfiguration config,
			PersistentEntity<?, ?> persistentEntity) {
		if (null == persistentEntity) {
			return null;
		}
		Class<?> domainType = persistentEntity.getType();
		ResourceMapping mapping = (null != config ? config.getResourceMappingForDomainType(domainType) : null);
		return merge(domainType, mapping);
	}

	public static ResourceMapping merge(Method method, ResourceMapping mapping) {
		ResourceMapping defaultMapping = new ResourceMapping(findRel(method), findPath(method), findExported(method));
		if (null != mapping) {
			return new ResourceMapping((null != mapping.getRel() ? mapping.getRel() : defaultMapping.getRel()),
					(null != mapping.getPath() ? mapping.getPath() : defaultMapping.getPath()),
					(mapping.isExported() != defaultMapping.isExported() ? mapping.isExported() : defaultMapping.isExported()));
		}
		return defaultMapping;
	}

	public static ResourceMapping merge(Class<?> type, ResourceMapping mapping) {
		ResourceMapping defaultMapping = new ResourceMapping(findRel(type), findPath(type), findExported(type));
		if (null != mapping) {
			return new ResourceMapping((null != mapping.getRel() ? mapping.getRel() : defaultMapping.getRel()),
					(null != mapping.getPath() ? mapping.getPath() : defaultMapping.getPath()),
					(mapping.isExported() != defaultMapping.isExported() ? mapping.isExported() : defaultMapping.isExported()))
					.addResourceMappings(mapping.getResourceMappings());
		}
		return defaultMapping;
	}

}
