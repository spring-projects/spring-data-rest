package org.springframework.data.rest.test.webmvc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.repository.annotation.RestPathSegment;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@RestPathSegment("person")
public interface PersonRepository extends CrudRepository<Person, Long> {
}
