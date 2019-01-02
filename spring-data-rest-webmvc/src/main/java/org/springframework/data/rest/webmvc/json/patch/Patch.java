/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.json.patch;

import java.util.List;

/**
 * <p>
 * Represents a Patch.
 * </p>
 * <p>
 * This class (and {@link PatchOperation} capture the definition of a patch, but are not coupled to any specific patch
 * representation.
 * </p>
 * 
 * @author Craig Walls
 * @author Oliver Gierke
 */
public class Patch {

	private final List<PatchOperation> operations;

	public Patch(List<PatchOperation> operations) {
		this.operations = operations;
	}

	/**
	 * @return the number of operations that make up this patch.
	 */
	public int size() {
		return operations.size();
	}

	public List<PatchOperation> getOperations() {
		return operations;
	}

	/**
	 * Applies the Patch to a given Object graph. Makes a copy of the given object so that it will remain unchanged after
	 * application of the patch and in case any errors occur while performing the patch.
	 * 
	 * @param in The object graph to apply the patch to.
	 * @param type The object type.
	 * @param <T> the object type.
	 * @return An object graph modified by the patch.
	 * @throws PatchException if there are any errors while applying the patch.
	 */
	public <T> T apply(T in, Class<T> type) throws PatchException {

		for (PatchOperation operation : operations) {
			operation.perform(in, type);
		}

		return in;
	}

	/**
	 * Applies the Patch to a given List of objects. Makes a copy of the given list so that it will remain unchanged after
	 * application of the patch and in case any errors occur while performing the patch.
	 * 
	 * @param in The list to apply the patch to.
	 * @param type The list's generic type.
	 * @param <T> the list's generic type.
	 * @return An list modified by the patch.
	 * @throws PatchException if there are any errors while applying the patch.
	 */
	public <T> List<T> apply(List<T> in, Class<T> type) throws PatchException {

		for (PatchOperation operation : operations) {
			operation.perform(in, type);
		}

		return in;
	}
}
