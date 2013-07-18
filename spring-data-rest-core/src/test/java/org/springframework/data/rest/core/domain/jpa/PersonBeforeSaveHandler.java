package org.springframework.data.rest.core.domain.jpa;

import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;

/**
 * @author Jon Brisbin
 */
public class PersonBeforeSaveHandler extends AbstractRepositoryEventListener<Person> {
	@Override
	protected void onBeforeSave(Person person) {
		throw new RuntimeException();
	}
}
