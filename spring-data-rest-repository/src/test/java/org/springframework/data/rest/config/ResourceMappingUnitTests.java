package org.springframework.data.rest.config;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.repository.domain.jpa.AnnotatedPersonRepository;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;
import org.springframework.data.rest.repository.domain.jpa.PlainPersonRepository;

/**
 * Ensure the {@link ResourceMapping} components convey the correct information.
 *
 * @author Jon Brisbin
 */
public class ResourceMappingUnitTests {

  @Test
  public void shouldDetectDefaultRelAndPath() throws Exception {
    ResourceMapping mapping = new ResourceMapping(
        findRel(PlainPersonRepository.class),
        findPath(PlainPersonRepository.class),
        findExported(PlainPersonRepository.class)
    );

    assertThat(mapping.getRel(), is("plainPerson"));
    assertThat(mapping.getPath(), is("plainPerson"));
    assertThat(mapping.isExported(), is(true));
  }

  @Test
  public void shouldDetectAnnotatedRelAndPath() throws Exception {
    ResourceMapping mapping = new ResourceMapping(
        findRel(AnnotatedPersonRepository.class),
        findPath(AnnotatedPersonRepository.class),
        findExported(AnnotatedPersonRepository.class)
    );

    assertThat(mapping.getRel(), is("people"));
    // The path is not set on  the annotation so this should be the default from class name.
    assertThat(mapping.getPath(), is("annotatedPerson"));
    assertThat(mapping.isExported(), is(false));
  }

  @Test
  public void shouldDetectAnnotatedRelAndPathOnMethod() throws Exception {
    Method method = PersonRepository.class.getMethod("findByFirstName", String.class, Pageable.class);
    ResourceMapping mapping = new ResourceMapping(
        findRel(method),
        findPath(method),
        findExported(method)
    );

    assertThat(mapping.getRel(), is("firstname"));
    assertThat(mapping.getPath(), is("firstname"));
    assertThat(mapping.isExported(), is(true));
  }

}
