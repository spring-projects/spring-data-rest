package org.springframework.data.rest.webmvc.spec

import org.springframework.core.MethodParameter
import org.springframework.data.rest.test.webmvc.ApplicationConfig
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration
import org.springframework.data.rest.webmvc.ResourceProcessorHandlerMethodReturnValueHandler
import org.springframework.hateoas.Link
import org.springframework.hateoas.Resource
import org.springframework.hateoas.ResourceProcessor
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import spock.lang.Specification

/**
 * @author Jon Brisbin
 */
@ContextConfiguration(classes = [ApplicationConfig, RepositoryRestMvcConfiguration])
class ResourceProcessorSpec extends Specification {

  static STRING_RESOURCE_PARAM = new MethodParameter(ResourceProcessorSpec.getMethod("createStringResource"), -1)
  static LONG_RESOURCE_PARAM = new MethodParameter(ResourceProcessorSpec.getMethod("createLongResource"), -1)
  static SPECIAL_STRING_RESOURCE_PARAM = new MethodParameter(ResourceProcessorSpec.getMethod("createSpecialStringResource"), -1)
  static SPECIAL_LONG_RESOURCE_PARAM = new MethodParameter(ResourceProcessorSpec.getMethod("createSpecialLongResource"), -1)

  HandlerMethodReturnValueHandler delegateHandler
  List<ResourceProcessor<?>> processors = []
  boolean handleReturnValueCalled
  HandlerMethodReturnValueHandler resourceHandler

  def setup() {
    delegateHandler = Mock(HandlerMethodReturnValueHandler)
    delegateHandler.handleReturnValue(_, _, null, null) >> { handleReturnValueCalled = true }

    processors << new SpecialStringResourceProcessor() <<
        new SpecialLongResourceProcessor() <<
        new StringResourceProcessor() <<
        new LongResourceProcessor()

    resourceHandler = new ResourceProcessorHandlerMethodReturnValueHandler(delegateHandler, processors)
  }

  Resource<String> createStringResource() {
    new Resource<String>("string-resource")
  }

  Resource<Long> createLongResource() {
    new Resource<Long>(1L)
  }

  StringResource createSpecialStringResource() {
    new StringResource("special-string-resource")
  }

  LongResource createSpecialLongResource() {
    new LongResource(1L)
  }

  def "processes simple String resource"() {

    given:
    def resource = createStringResource()

    when:
    resourceHandler.handleReturnValue(resource, STRING_RESOURCE_PARAM, null, null)

    then:
    null != resource.getLink("string-resource")
    handleReturnValueCalled

  }

  def "process simple Long resource"() {

    given:
    def resource = createLongResource()

    when:
    resourceHandler.handleReturnValue(resource, LONG_RESOURCE_PARAM, null, null)

    then:
    null != resource.getLink("long-resource")
    handleReturnValueCalled

  }

  def "process specialized String resource"() {

    given:
    def resource = createSpecialStringResource()

    when:
    resourceHandler.handleReturnValue(resource, SPECIAL_STRING_RESOURCE_PARAM, null, null)

    then:
    null != resource.getLink("special-string-resource")
    handleReturnValueCalled

  }

  def "process specialized Long resource"() {

    given:
    def resource = createSpecialLongResource()

    when:
    resourceHandler.handleReturnValue(resource, SPECIAL_LONG_RESOURCE_PARAM, null, null)

    then:
    null != resource.getLink("special-long-resource")
    handleReturnValueCalled

  }

}

class StringResourceProcessor implements ResourceProcessor<Resource<String>> {
  @Override Resource<String> process(Resource<String> resource) {
    resource.add(new Link("http://localhost:8080/string-resource", "string-resource"))
    resource
  }
}

class LongResourceProcessor implements ResourceProcessor<Resource<Long>> {
  @Override Resource<Long> process(Resource<Long> resource) {
    resource.add(new Link("http://localhost:8080/long-resource", "long-resource"))
    resource
  }
}

class StringResource extends Resource<String> {
  StringResource(String content, Link... links) {
    super(content, links)
  }
}

class SpecialStringResourceProcessor implements ResourceProcessor<StringResource> {
  @Override StringResource process(StringResource resource) {
    resource.add(new Link("http://localhost:8080/special-string-resource", "special-string-resource"))
    resource
  }
}

class LongResource extends Resource<Long> {
  LongResource(Long content, Link... links) {
    super(content, links)
  }
}

class SpecialLongResourceProcessor implements ResourceProcessor<LongResource> {
  @Override LongResource process(LongResource resource) {
    resource.add(new Link("http://localhost:8080/special-long-resource", "special-long-resource"))
    resource
  }
}