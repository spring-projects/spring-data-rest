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
package org.springframework.data.rest.repository.mapping;

/**
 * Mapping information for components to be exported as REST resources.
 * 
 * @author Oliver Gierke
 */
public interface ResourceMapping {
	
	public static ResourceMapping NO_MAPPING = new ResourceMapping() {
		
		@Override
		public Boolean isExported() {
			return false;
		}
		
		@Override
		public String getRel() {
			return null;
		}
		
		@Override
		public String getPath() {
			return null;
		}
	};

	/**
	 * Returns whether the component shall be exported at all.
	 * 
	 * @return will never be {@literal null}.
	 */
	Boolean isExported();

	/**
	 * Returns the relation for the resource exported.
	 * 
	 * @return will never be {@literal null}.
	 */
	String getRel();

	/**
	 * Returns the path the resource is exposed under.
	 * 
	 * @return will never be {@literal null}.
	 */
	String getPath();
}
