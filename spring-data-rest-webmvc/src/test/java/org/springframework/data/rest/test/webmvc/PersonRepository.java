package org.springframework.data.rest.test.webmvc;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.annotation.RestPathSegment;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@RestPathSegment("person")
public interface PersonRepository extends CrudRepository<Person, Long> {

  @RestPathSegment("byName")
  public List<Person> findByName(@Param("nme") String name);

}
