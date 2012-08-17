package org.springframework.data.rest.webmvc.spec

import org.springframework.data.rest.test.webmvc.Customer
import org.springframework.http.HttpStatus

/**
 * @author Jon Brisbin
 */
class NestedObjectSpec extends BaseSpec {

  def "saves nested object"() {

    given:
    def customer = customers.save(new Customer(userid: "jdoe"))
    def jsonObj = [
        "customers": [
            ["rel": "customer.Customer", "href": "http://localhost:8080/data/customer/" + customer.id]
        ]
    ]
    def request = createJsonRequest("PUT", "customerTracker/1", null, jsonObj)
    def getReq = createRequest("GET", "customerTracker/1/customers", null)

    when:
    def response = controller.create(request, baseUri, "customerTracker")

    then:
    response.statusCode == HttpStatus.CREATED

    when:
    def getResp = controller.propertyOfEntity(getReq, baseUri, "customerTracker", "1", "customers")
    def jsonResp = readJson(getResp)

    then:
    getResp.statusCode == HttpStatus.OK
    jsonResp.content[0].userid == "jdoe"

  }

}
