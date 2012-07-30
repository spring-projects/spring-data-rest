package org.springframework.data.rest.webmvc.spec

import org.springframework.data.domain.PageRequest
import org.springframework.data.rest.test.webmvc.Person
import org.springframework.data.rest.webmvc.PagingAndSorting
import org.springframework.data.rest.webmvc.RepositoryRestConfiguration
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared

import java.lang.reflect.InvocationTargetException

/**
 * @author Jon Brisbin
 */
class QueryMethodsSpec extends BaseSpec {

  @Shared
  def pageSort = new PagingAndSorting(RepositoryRestConfiguration.DEFAULT, new PageRequest(0, 10))

  def "exposes query method links to discovery"() {

    given:
    def request = createRequest("GET", "people/search", null)

    when:
    def response = controller.listQueryMethods(request, baseUri, "people")
    def body = readJson(response)

    then:
    response.statusCode == HttpStatus.OK
    body["_links"].size() == 2

  }

  @Transactional
  def "invokes query methods"() {

    given:
    people.save(new Person(name: "John Doe"))
    people.save(new Person(name: "Bill Doe"))
    def request = createRequest("GET", "people/search/nameStartsWith", ["name": "John"])

    when:
    def response = controller.query(request, pageSort, baseUri, "people", "nameStartsWith")
    def body = readJson(response)

    then:
    response.statusCode == HttpStatus.OK
    body["results"].size() == 1

  }

  @Transactional
  def "blows up on empty query parameters"() {

    given:
    def request = createRequest("GET", "people/search/nameStartsWith", null)

    when:
    controller.query(request, pageSort, baseUri, "people", "nameStartsWith")

    then:
    thrown(InvocationTargetException)

  }

}
