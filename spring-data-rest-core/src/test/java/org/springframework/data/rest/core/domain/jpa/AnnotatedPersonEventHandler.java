/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.rest.core.domain.jpa;

import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterLinkDelete;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RepositoryEventHandler(Person.class)
public class AnnotatedPersonEventHandler {

	@HandleAfterCreate
	@HandleAfterDelete
	@HandleAfterSave
	public void handleAfter(Person p) {
		throw new EventHandlerInvokedException();
	}

	@HandleAfterLinkDelete
	@HandleAfterLinkSave
	public void handleAfterLink(Person p, Object o) {
		throw new EventHandlerInvokedException();
	}

	@HandleBeforeCreate
	@HandleBeforeDelete
	@HandleBeforeSave
	public void handleBefore(Person p) {
		throw new EventHandlerInvokedException();
	}

	@HandleBeforeLinkDelete
	@HandleBeforeLinkSave
	public void handleBeforeLink(Person p, Object o) {
		throw new EventHandlerInvokedException();
	}
}
