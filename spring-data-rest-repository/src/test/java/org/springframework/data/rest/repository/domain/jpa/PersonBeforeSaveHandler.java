package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.data.rest.repository.context.AbstractRepositoryEventListener;

/**
 * @author Jon Brisbin
 */
public class PersonBeforeSaveHandler extends AbstractRepositoryEventListener<Person> {
  @Override protected void onBeforeSave(Person person) {
    throw new RuntimeException();
  }
}
