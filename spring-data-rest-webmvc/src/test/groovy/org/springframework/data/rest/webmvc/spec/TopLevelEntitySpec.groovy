package org.springframework.data.rest.webmvc.spec

import org.springframework.data.rest.test.webmvc.Person
import org.springframework.http.HttpStatus

/**
 * @author Jon Brisbin
 */
class TopLevelEntitySpec extends BaseSpec {

  def "saves top-level entity"() {

    given:
    def person = new Person(name: "John Doe")
    def request = createJsonRequest("POST", "people/1", null, person)

    when:
    def response = controller.createOrUpdate(request, baseUri, "people", "1")

    then:
    response.statusCode == HttpStatus.CREATED

  }

  def "retrieves top-level entity"() {

    given:
    def person = newPerson()
    def request = createRequest("GET", "people/${person.id}", null)

    when:
    def response = controller.entity(request, baseUri, "people", "${person.id}")

    then:
    response.statusCode == HttpStatus.OK

  }

}
