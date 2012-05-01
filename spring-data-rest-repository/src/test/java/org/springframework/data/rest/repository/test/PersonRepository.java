package org.springframework.data.rest.repository.test;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.repository.annotation.RestPathSegment;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface PersonRepository extends CrudRepository<Person, Long> {

  @RestPathSegment("byName")
  public List<Person> findByName(String name);

}
