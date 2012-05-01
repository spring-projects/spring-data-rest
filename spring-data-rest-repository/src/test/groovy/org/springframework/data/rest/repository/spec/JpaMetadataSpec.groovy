package org.springframework.data.rest.repository.spec

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.repository.CrudRepository
import org.springframework.data.rest.repository.RepositoryExporter
import org.springframework.data.rest.repository.RepositoryMetadata
import org.springframework.data.rest.repository.test.Family
import org.springframework.data.rest.repository.test.FamilyRepository
import org.springframework.data.rest.repository.test.Person
import org.springframework.data.rest.repository.test.PersonRepository
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
  @Autowired
  List<RepositoryExporter> exporters

  RepositoryMetadata repositoryMetadataFor(name) {
    exporters.find { null != it.repositoryMetadataFor(name) }?.repositoryMetadataFor(name)
  }

  def "finds repositories in ApplicationContext"() {

    when: "find repo by String identifier"
    def repo = repositoryMetadataFor("person").repository()

    then:
    null != repo
    repo instanceof PersonRepository

    when: "find repo by domain Class<?>"
    repo = repositoryMetadataFor(Family).repository()

    then:
    null != repo
    repo instanceof FamilyRepository

  }

  def "provides entity metadata"() {

    given:
    def personRepo = repositoryMetadataFor(Person).repository()
    def familyRepo = repositoryMetadataFor(Family).repository()
    def johnDoe = personRepo.save(new Person("John Doe"))
    def janeDoe = personRepo.save(new Person("Jane Doe"))
    def doeFamily = familyRepo.save(new Family(
        surname: "Doe",
        members: [johnDoe, janeDoe]
    ))

    when:
    def personMeta = repositoryMetadataFor(Person).entityMetadata()
    def familyMeta = repositoryMetadataFor(Family).entityMetadata()

    then:
    personMeta.attribute("name").get(johnDoe) == "John Doe"
    familyMeta.attribute("surname").get(doeFamily) == "Doe"
    familyMeta.attribute("members").get(doeFamily).size() == 2
    personMeta.embeddedAttributes().size() == 1
    familyMeta.linkedAttributes().size() == 1

  }

}
