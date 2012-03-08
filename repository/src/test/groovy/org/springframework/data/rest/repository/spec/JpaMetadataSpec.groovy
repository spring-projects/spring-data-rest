package org.springframework.data.rest.repository.spec

import javax.persistence.Entity
import javax.persistence.EntityManager
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.repository.CrudRepository
import org.springframework.data.rest.repository.JpaRepositoryMetadata
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@ContextConfiguration(locations = ["/JpaMetadataSpec-test.xml"])
class JpaMetadataSpec extends Specification {

  @Autowired
  ApplicationContext applicationContext
  @PersistenceContext
  EntityManager entityManager
  @Autowired
  Collection<CrudRepository> repositories
  JpaRepositoryMetadata repoMeta

  def setup() {
    repoMeta = new JpaRepositoryMetadata(
        repositories: repositories,
        applicationContext: applicationContext,
        entityManager: entityManager
    )
    repoMeta.afterPropertiesSet()
  }

  def "finds repositories in ApplicationContext"() {

    when:
    def repo = repoMeta.repositoryFor("simple")

    then:
    null != repo
    repo instanceof SimpleRepository

    when:
    repo = repoMeta.repositoryFor(Simple)

    then:
    null != repo
    repo instanceof SimpleRepository

  }

}

@Entity
class Simple {
  @Id @GeneratedValue Long id
  String name
}

interface SimpleRepository extends CrudRepository<Simple, Long> {}
