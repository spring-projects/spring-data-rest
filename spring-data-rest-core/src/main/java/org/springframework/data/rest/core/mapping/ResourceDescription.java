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
package org.springframework.data.rest.core.mapping;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;

/**
 * A description of a resource. Resolvable to plain text by using a {@link MessageSource}.
 * 
 * @author Oliver Gierke
 */
public interface ResourceDescription extends MessageSourceResolvable {

	/**
	 * Returns the description. This can be a message source code or a custom text format. Prefer resolving the
	 * {@link ResourceDescription} using a {@link MessageSource}.
	 * 
	 * @return
	 */
	String getMessage();

	/**
	 * Returns whether this is the default description.
	 * 
	 * @return
	 */
	boolean isDefault();

	MediaType getType();
}
