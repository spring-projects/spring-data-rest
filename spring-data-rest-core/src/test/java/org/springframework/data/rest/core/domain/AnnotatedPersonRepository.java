package org.springframework.data.rest.core.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * A repository to manage {@link org.springframework.data.rest.core.domain.Person}s.
 * 
 * @author Jon Brisbin
 */
@RestResource(rel = "people", exported = false)
@NoRepositoryBean
public interface AnnotatedPersonRepository extends CrudRepository<Person, Long> {}
