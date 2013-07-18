package org.springframework.data.rest.core.domain.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * A repository to manage {@link Person}s.
 * 
 * @author Jon Brisbin
 */
@NoRepositoryBean
public interface PlainPersonRepository extends CrudRepository<Person, Long> {}
