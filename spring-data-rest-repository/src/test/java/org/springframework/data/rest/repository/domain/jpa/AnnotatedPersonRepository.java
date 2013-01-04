package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * A repository to manage {@link org.springframework.data.rest.repository.domain.jpa.Person}s.
 *
 * @author Jon Brisbin
 */
@RestResource(rel = "people", exported = false)
public interface AnnotatedPersonRepository extends CrudRepository<Person, Long> {
}
