package org.springframework.data.rest.repository.jpa;

import java.io.Serializable;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.RepositoryExporter;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class JpaRepositoryExporter extends RepositoryExporter<
    JpaRepositoryMetadata<Repository<Object, Serializable>>,
    Repository<Object, Serializable>,
    JpaEntityMetadata> {

  @SuppressWarnings({"unchecked"})
  @Override
  protected JpaRepositoryMetadata<Repository<Object, Serializable>> createRepositoryMetadata(
      Class repoClass,
      Repository<Object, Serializable> repo,
      String name,
      EntityInformation entityInfo) {
    return new JpaRepositoryMetadata(new Repositories(applicationContext),
                                     name,
                                     repoClass,
                                     repo,
                                     entityInfo,
                                     entityManager);
  }

}
