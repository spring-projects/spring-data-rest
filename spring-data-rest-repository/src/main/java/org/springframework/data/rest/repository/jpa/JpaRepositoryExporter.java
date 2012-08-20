package org.springframework.data.rest.repository.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.RepositoryExporter;

/**
 * Implementation of {@link RepositoryExporter} for exporting JPA {@link Repository} subinterfaces.
 *
 * @author Jon Brisbin
 */
public class JpaRepositoryExporter
    extends RepositoryExporter<JpaRepositoryExporter, JpaRepositoryMetadata, JpaEntityMetadata> {

  protected EntityManager entityManager;

  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  protected JpaRepositoryMetadata createRepositoryMetadata(String name, Class<?> domainType, Class<?> repoClass, Repositories repositories) {
    return new JpaRepositoryMetadata(name, domainType, repoClass, repositories, entityManager);
  }

}
