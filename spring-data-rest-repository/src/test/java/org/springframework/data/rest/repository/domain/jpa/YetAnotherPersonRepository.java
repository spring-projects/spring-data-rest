package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.repository.annotation.RestResource;

import java.util.Date;

/**
 * A repository to manage {@link org.springframework.data.rest.repository.domain.jpa.Person}s.
 * Used in leading-slash paths and method export tests.
 *
 * @author Florent Biville
 */
@RestResource(path = "/people")
public interface YetAnotherPersonRepository {

  @RestResource(path = "/firstname")
  public Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

  @RestResource(path = " / ")
  public Page<Person> findByLastName(@Param("lastName") String firstName, Pageable pageable);

  @RestResource(exported = false)
  public Person findByFirstNameLike(String firstName);

  @RestResource(path = "/createdAfter")
  public Person findByCreatedAfter(@Param("date") Date creationDate);

  @RestResource(path = "/createdBefore") /* wont be exported */
  public Person findByCreatedBefore(Date creationDate);

  @RestResource(path = "/")
  public Person findAll(Pageable paging);

  @RestResource(path = "/")
  public Person findAll(Sort sorting);

  @RestResource(path = "/") /* wont be exported */
  public Person findByCreatedAfterAndLastName(Date creationDate,
                                              @Param("lastName") String lastName,
                                              Sort sorting);

  @RestResource(path = "/")
  public Person findByCreatedAfterAndFirstName(@Param("creationDate") Date creationDate,
                                              @Param("firstName") String firstName,
                                              Pageable paging);
}
