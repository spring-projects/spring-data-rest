package org.springframework.data.rest.webmvc.spec

import org.springframework.data.rest.test.webmvc.Person
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional

/**
 * @author Jon Brisbin
 */
class TopLevelEntitySpec extends BaseSpec {

  @Transactional
  def "saves top-level entity"() {

    given:
    def person = new Person(name: "John Doe")
    def request = createJsonRequest("POST", "people/1", null, person)

    when:
    def response = controller.createOrUpdate(request, baseUri, "people", "1")

    then:
    response.statusCode == HttpStatus.CREATED

  }

  @Transactional
  def "retrieves top-level entity"() {

    given:
    def person = newPerson()
    def request = createRequest("GET", "people/${person.id}", null)

    when:
    def response = controller.entity(request, baseUri, "people", "${person.id}")

    then:
    response.statusCode == HttpStatus.OK

  }

  @Transactional
  def "updates top-level entity"() {

    given:
    def person = newPerson()
    person.name = "Johnnie Doe"
    person = people.save(person)
    def persId = person.id
    def request = createJsonRequest("PUT", "people/$persId", null, person)
    def retrReq = createRequest("GET", "people/$persId", null)

    when:
    def response = controller.createOrUpdate(request, baseUri, "people", "$persId")

    then:
    response.statusCode == HttpStatus.NO_CONTENT

    when:
    response = controller.entity(retrReq, baseUri, "people", "$persId")

    then:
    response.statusCode == HttpStatus.OK

    when:
    def pers = readJson(response)

    then:
    pers.name == "Johnnie Doe"

  }

  @Transactional
  def "won't delete entities whose delete methods are not exported"() {

    given:
    def person = people.save(new Person(name: "John Doe"))
    def persId = person.id
    def request = createRequest("DELETE", "people/$persId", null)

    when:
    def response = controller.deleteEntity(request, "people", "$persId")

    then:
    response.statusCode == HttpStatus.METHOD_NOT_ALLOWED

  }

}
