package org.springframework.data.rest.webmvc.spec

import org.codehaus.jackson.map.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.rest.test.webmvc.Address
import org.springframework.data.rest.test.webmvc.AddressRepository
import org.springframework.data.rest.test.webmvc.ApplicationConfig
import org.springframework.data.rest.test.webmvc.CustomerRepository
import org.springframework.data.rest.test.webmvc.Person
import org.springframework.data.rest.test.webmvc.PersonRepository
import org.springframework.data.rest.test.webmvc.TestRepositoryEventListener
import org.springframework.data.rest.webmvc.RepositoryRestController
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import spock.lang.Specification

import javax.persistence.EntityManagerFactory

import static org.springframework.transaction.support.TransactionSynchronizationManager.*

/**
 * @author Jon Brisbin
 */
@ContextConfiguration(classes = [ApplicationConfig, RepositoryRestMvcConfiguration])
abstract class BaseSpec extends Specification {

  @Autowired ApplicationContext appCtx
  @Autowired TestRepositoryEventListener listener
  @Autowired RepositoryRestController controller
  @Autowired EntityManagerFactory emf
  @Autowired PersonRepository people
  @Autowired AddressRepository addresses
  @Autowired CustomerRepository customers
  UriComponentsBuilder baseUri

  def mapper = new ObjectMapper()

  @Transactional
  def setup() {
    baseUri = UriComponentsBuilder.fromUriString("http://localhost:8080/data")

    if (!hasResource(emf)) {
      bindResource(emf, new EntityManagerHolder(emf.createEntityManager()))
    }

    for (Address a : addresses.findAll()) {
      addresses.delete(a)
    }
    for (Person p : people.findAll()) {
      people.delete(p)
    }
  }

  def readJson(ResponseEntity entity) {
    mapper.readValue((byte[]) entity.body, Map)
  }

  def createJsonRequest(method, path, query, obj) {
    createRequest(method, path, query, "application/json", mapper.writeValueAsString(obj))
  }

  def createUriListRequest(method, path, query, obj) {
    createRequest(method, path, query, "text/uri-list", obj.join("\n"))
  }

  def createRequest(method, path, query) {
    createRequest(method, path, query, null, null)
  }

  def createRequest(method, path, query, contentType, content) {
    def req = new MockHttpServletRequest(
        serverPort: 8080,
        requestURI: "/data/$path",
        method: method
    )
    if (query) {
      query.collect { String k, String v -> req.addParameter(k, v)}
    }
    if (contentType) {
      req.contentType = contentType
    }
    if (content) {
      req.content = content
    }

    new ServletServerHttpRequest(req)
  }

  def newPerson() {
    def p = people.save(new Person(name: "John Doe"))
    def a = newAddress("Univille", p)
    p.addresses = [a]
    people.save(p)
  }

  def newAddress(city, person) {
    addresses.save(new Address(
        ["1234 W. 1st St."] as String[],
        city,
        "ST",
        "12345",
        person
    ))
  }

}
