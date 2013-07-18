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
 */
@RepositoryEventHandler(Person.class)
public class AnnotatedPersonEventHandler {
	@HandleAfterCreate
	@HandleAfterDelete
	@HandleAfterSave
	public void handleAfter(Person p) {
		throw new RuntimeException();
	}

	@HandleAfterLinkDelete
	@HandleAfterLinkSave
	public void handleAfterLink(Person p, Object o) {
		throw new RuntimeException();
	}

	@HandleBeforeCreate
	@HandleBeforeDelete
	@HandleBeforeSave
	public void handleBefore(Person p) {
		throw new RuntimeException();
	}

	@HandleBeforeLinkDelete
	@HandleBeforeLinkSave
	public void handleBeforeLink(Person p, Object o) {
		throw new RuntimeException();
	}
}
