package org.springframework.data.rest.webmvc.spec

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.rest.repository.RepositoryConstraintViolationException
import org.springframework.data.rest.test.webmvc.Customer
import org.springframework.data.rest.test.webmvc.Person
import org.springframework.data.rest.test.webmvc.TestRepositoryEventListener
import org.springframework.http.HttpStatus

import javax.validation.ConstraintViolationException

/**
 * @author Jon Brisbin
 */
class EventsSpec extends BaseSpec {

  @Autowired TestRepositoryEventListener listener

  def "cannot save invalid entity"() {

    given:
    def person = new Person()
    def request = createJsonRequest("POST", "people", null, person)

    when:
    try {
      controller.create(request, baseUri, "people")
    } catch (RepositoryConstraintViolationException e) {
      controller.handleValidationFailure(e, request)
      throw e
    }

    then:
    thrown(RepositoryConstraintViolationException)

  }

  def "handles JSR-303 validation errors"() {

    given:
    def cust = new Customer()
    def request = createJsonRequest("POST", "customer", null, cust)

    when:
    try {
      controller.create(request, baseUri, "customer")
    } catch (ConstraintViolationException e) {
      controller.handleJsr303ValidationFailure(e, request)
      throw e
    }

    then:
    thrown(ConstraintViolationException)

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

}
