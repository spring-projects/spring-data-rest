package org.springframework.data.rest.repository.jpa;

import java.io.Serializable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.rest.repository.RepositoryExporter;

/**
 * Implementation of {@link RepositoryExporter} for exporting JPA {@link Repository} subinterfaces.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class JpaRepositoryExporter extends RepositoryExporter<
    JpaRepositoryMetadata<Repository<Object, Serializable>>,
    Repository<Object, Serializable>,
    JpaEntityMetadata> {

  protected EntityManager entityManager;

  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  protected JpaRepositoryMetadata<Repository<Object, Serializable>> createRepositoryMetadata(
      Class repoClass,
      Repository<Object, Serializable> repo,
      String name,
      EntityInformation entityInfo) {
    return new JpaRepositoryMetadata(repositories,
                                     name,
                                     repoClass,
                                     repo,
                                     entityInfo,
                                     entityManager);
  }

}
