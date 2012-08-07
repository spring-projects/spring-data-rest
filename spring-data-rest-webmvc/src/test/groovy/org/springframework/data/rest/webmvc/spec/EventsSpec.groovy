package org.springframework.data.rest.webmvc.spec

import org.springframework.data.rest.repository.RepositoryConstraintViolationException
import org.springframework.data.rest.test.webmvc.Person
import org.springframework.http.HttpStatus

/**
 * @author Jon Brisbin
 */
class EventsSpec extends BaseSpec {

  def "cannot save invalid entity"() {

    given:
    def person = new Person()
    def request = createJsonRequest("POST", "people", null, person)

    when:
    controller.create(request, baseUri, "people")

    then:
    thrown(RepositoryConstraintViolationException)

  }

  def "captures before and after events"() {

    given:
    def person = new Person(name: "John Doe")
    def request = createJsonRequest("POST", "people", ["returnBody": "true"], person)
    def persId
    listener.handlers << { evt, p ->
      if (evt == "afterSave")
        persId = "${p.id}"
    }

    when:
    def response = controller.create(request, baseUri, "people")
    def returnedId = response.headers.getFirst('Location').tokenize("/").last()

    then:
    response.statusCode == HttpStatus.CREATED
    persId == returnedId

  }

  def "captures resource rendering events"() {

  }

}
