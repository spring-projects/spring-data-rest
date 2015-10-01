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
package org.springframework.data.rest.webmvc.json.patch;

/**
 * Abstract base class for operations requiring a source property, such as "copy" and "move".
 * (e.g., copy <i>from</i> here to there.
 * @author Craig Walls
 */
public abstract class FromOperation extends PatchOperation {

	protected String from;
	
	/**
	 * Constructs the operation
	 * @param op The name of the operation to perform. (e.g., 'copy')
	 * @param path The operation's target path. (e.g., '/foo/bar/4')
	 * @param from The operation's source path. (e.g., '/foo/bar/5')
	 */
	public FromOperation(String op, String path, String from) {
		super(op, path);
		this.from = from;
	}
	
	public String getFrom() {
		return from;
	}

}
