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

import org.springframework.context.MessageSourceResolvable;

/**
 * Adapter class for the {@link MessageSourceResolvable} part of a {@link ResourceDescription}.
 * 
 * @author Oliver Gierke
 */
public abstract class ResolvableResourceDescriptionSupport implements ResourceDescription {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.MessageSourceResolvable#getCodes()
	 */
	@Override
	public String[] getCodes() {
		return new String[] { getMessage() };
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.MessageSourceResolvable#getArguments()
	 */
	@Override
	public Object[] getArguments() {
		return new Object[0];
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.MessageSourceResolvable#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return null;
	}
}
