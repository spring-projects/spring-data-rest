package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.data.repository.CrudRepository;

/**
 * A repository to manage {@link Person}s.
 *
 * @author Jon Brisbin
 */
public interface PlainPersonRepository extends CrudRepository<Person, Long> {
}
