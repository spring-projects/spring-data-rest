/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.rest.core.event;

/**
 * Base class for {@link RepositoryEvent}s that deal with saving/updating or deleting a linked object.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public abstract class LinkedEntityEvent extends RepositoryEvent {

	private static final long serialVersionUID = -9071648572128698903L;
	private final Object linked;

	/**
	 * Creates a new {@link LinkedEntityEvent} for th given source and linked instance.
	 * 
	 * @param source must not be {@literal null}.
	 * @param linked can be {@literal null}.
	 */
	public LinkedEntityEvent(Object source, Object linked) {

		super(source);
		this.linked = linked;
	}

	/**
	 * Get the linked object.
	 * 
	 * @return The entity representing the right-hand side of this relationship.
	 */
	public Object getLinked() {
		return linked;
	}
}
