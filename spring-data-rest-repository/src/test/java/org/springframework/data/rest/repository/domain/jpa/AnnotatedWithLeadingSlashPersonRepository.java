package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * A repository to manage {@link org.springframework.data.rest.repository.domain.jpa.Person}s.
 *
 * @author Florent Biville
 */
@RestResource(path = "/people")
public interface AnnotatedWithLeadingSlashPersonRepository {

  @RestResource(path = "/firstname")
  public Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

  @RestResource(path = " / ")
  public Page<Person> findByLastName(@Param("lastName") String firstName, Pageable pageable);
}
