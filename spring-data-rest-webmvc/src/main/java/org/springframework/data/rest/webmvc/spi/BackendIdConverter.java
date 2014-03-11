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
package org.springframework.data.rest.webmvc.spi;

import java.io.Serializable;

import org.springframework.plugin.core.Plugin;

/**
 * SPI to allow the customization of how entity ids are exposed in URIs generated.
 * 
 * @author Oliver Gierke
 */
public interface BackendIdConverter extends Plugin<Class<?>> {

	/**
	 * Returns the id of the entity to be looked up eventually.
	 * 
	 * @param id the source id as it was parsed from the incoming request, will never be {@literal null}.
	 * @param entityType the type of the object to be resolved, will never be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	Serializable fromRequestId(String id, Class<?> entityType);

	/**
	 * Returns the id to be used in the URI generated to point to an entity of the given type with the given id.
	 * 
	 * @param id the entity's id, will never be {@literal null}.
	 * @param entityType the type of the entity to expose.
	 * @return
	 */
	String toRequestId(Serializable id, Class<?> entityType);

	/**
	 * The default {@link BackendIdConverter} that will simply use ids as they are.
	 * 
	 * @author Oliver Gierke
	 */
	public enum DefaultIdConverter implements BackendIdConverter {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.support.BackendIdConverter#fromRequestId(java.lang.String, java.lang.Class)
		 */
		@Override
		public Serializable fromRequestId(String id, Class<?> entityType) {
			return id;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.support.BackendIdConverter#toRequestId(java.lang.Object, java.lang.Class)
		 */
		@Override
		public String toRequestId(Serializable id, Class<?> entityType) {
			return id.toString();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
		 */
		@Override
		public boolean supports(Class<?> delimiter) {
			return true;
		}
	}
}
