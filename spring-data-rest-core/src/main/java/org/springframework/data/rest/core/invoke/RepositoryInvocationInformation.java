/*
 * Copyright 2013-2014 the original author or authors.
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

/**
 * Meta-information about the methods a repository exposes.
 * 
 * @author Oliver Gierke
 */
public interface RepositoryInvocationInformation {

	/**
	 * Returns whether the repository has a method to save objects.
	 * 
	 * @return
	 */
	boolean hasSaveMethod();

	/**
	 * Returns whether the repository exposes the save method.
	 * 
	 * @return
	 */
	boolean exposesSave();

	/**
	 * Returns whether the repository has a method to delete objects.
	 * 
	 * @return
	 */
	boolean hasDeleteMethod();

	/**
	 * Returns whether the repository exposes the delete method.
	 * 
	 * @return
	 */
	boolean exposesDelete();

	/**
	 * Returns whether the repository has a method to find a single object.
	 * 
	 * @return
	 */
	boolean hasFindOneMethod();

	/**
	 * Returns whether the repository exposes the method to find a single object.
	 * 
	 * @return
	 */
	boolean exposesFindOne();

	/**
	 * Returns whether the repository has a method to find all objects.
	 * 
	 * @return
	 */
	boolean hasFindAllMethod();

	/**
	 * Returns whether the repository exposes the method to find all objects.
	 * 
	 * @return
	 */
	boolean exposesFindAll();
}
