package org.springframework.data.rest.repository.test;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface PersonRepository extends CrudRepository<Person, Long> {

  @RestResource(path = "byName")
  public List<Person> findByName(String name);

}
