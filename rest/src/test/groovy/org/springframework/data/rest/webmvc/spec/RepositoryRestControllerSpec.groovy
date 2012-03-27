package org.springframework.data.rest.webmvc.spec

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.ser.CustomSerializerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.rest.core.SimpleLink
import org.springframework.data.rest.core.util.FluentBeanSerializer
import org.springframework.data.rest.webmvc.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.RepositoryRestController
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.ExtendedModelMap
import org.springframework.ui.Model
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@ContextConfiguration(classes = [RepositoryRestConfiguration, RepositoryRestMvcConfiguration])
class RepositoryRestControllerSpec extends Specification {

  @Shared
  ObjectMapper mapper = new ObjectMapper()
  @Autowired
  URI baseUri
  @Autowired
  RepositoryRestController controller

  MockHttpServletRequest createRequest(String method, String path) {
    return new MockHttpServletRequest(
        serverPort: 8080,
        requestURI: "/data/$path",
        method: method
    )
  }

  Model GET(String path) {
    def request = createRequest("GET", path)
    def model = new ExtendedModelMap()
    controller.get(new ServletServerHttpRequest(request), model)
    return model
  }

  Model POST(String path, m) {
    def request = createRequest("POST", path)
    request.contentType = "application/json"
    request.setContent(mapper.writeValueAsBytes(m))
    def model = new ExtendedModelMap()
    controller.createOrUpdate(new ServletServerHttpRequest(request), model)
    return model
  }

  Model PUT(String path, m) {
    def request = createRequest("PUT", path)
    request.contentType = "application/json"
    request.setContent(mapper.writeValueAsBytes(m))
    def model = new ExtendedModelMap()
    controller.createOrUpdate(new ServletServerHttpRequest(request), model)
    return model
  }

  Model DELETE(String path) {
    def request = createRequest("DELETE", path)
    def model = new ExtendedModelMap()
    controller.delete(new ServletServerHttpRequest(request), model)
    return model

  }

  def setupSpec() {
    def customSerializerFactory = new CustomSerializerFactory()
    customSerializerFactory.addSpecificMapping(SimpleLink, new FluentBeanSerializer(SimpleLink))
    mapper.setSerializerFactory(customSerializerFactory)
  }

  @Transactional
  def "responds to GET"() {

    when:
    def repos = GET("")
    def reposLinks = repos.resource?._links

    then:
    repos.status == HttpStatus.OK
    reposLinks?.size() == 3

    when:
    def persons = GET("person")
    def personsLinks = persons.resource?._links

    then:
    persons.status == HttpStatus.OK
    personsLinks[0].href().toString() == "http://localhost:8080/data/person/1"

    when:
    def person = GET("person/1")

    then:
    person?.resource?.name == "John Doe"

    when:
    def profiles = GET("person/1/profiles")
    def profilesLinks = profiles.resource?.profiles

    then:
    profilesLinks.size() == 2

  }

  @Transactional
  def "responds to POST with ID"() {

    when:
    def created = POST("person/3", [name: "James Doe"])

    then:
    created.status == HttpStatus.CREATED

  }

  @Transactional
  def "responds to PUT"() {

    given:
    POST("person/3", [name: "James Doe"])

    when:
    def updated = PUT("person/3", [name: "James Doe Jr."])
    def getUpdated = GET("person/3")

    then:
    updated.status == HttpStatus.NO_CONTENT
    getUpdated.status == HttpStatus.OK
    getUpdated.resource?.name == "James Doe Jr."

  }

  @Transactional
  def "updates links"() {

    given:
    POST("person/3", [name: "James Doe"])

    when:
    def link = POST("person/3/addresses", [[href: "$baseUri/address/1".toString()]])
    def getUpdated = GET("person/3/addresses")

    then:
    link.status == HttpStatus.CREATED
    getUpdated.status == HttpStatus.OK
    getUpdated.resource?.size() == 1

  }

  @Transactional
  def "responds to DELETE"() {

    given:
    POST("person/3", [name: "James Doe"])
    POST("person/3/addresses", [[href: "$baseUri/address/1".toString()]])

    when:
    def deleted = DELETE("person/3/addresses/1")
    def getUpdated = GET("person/3/addresses")

    then:
    deleted.status == HttpStatus.NO_CONTENT
    getUpdated.status == HttpStatus.OK
    getUpdated.resource?._links?.size() == 0

    when:
    def delEntity = DELETE("person/3")
    def getUpdEntity = GET("person/3")

    then:
    delEntity.status == HttpStatus.NO_CONTENT
    getUpdEntity.status == HttpStatus.NOT_FOUND

  }

}
