package org.springframework.data.rest.config;

import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.repository.domain.jpa.AnnotatedPersonRepository;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;
import org.springframework.data.rest.repository.domain.jpa.PlainPersonRepository;
import org.springframework.data.rest.repository.domain.jpa.YetAnotherPersonRepository;
import org.springframework.data.rest.repository.support.ResourceMappingUtils;

import java.lang.reflect.Method;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

/**
 * Ensure the {@link ResourceMapping} components convey the correct information.
 *
 * @author Jon Brisbin
 * @author Florent Biville
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

  @Test
  public void shouldDetectPathAndRemoveLeadingSlashIfAny() {
    ResourceMapping mapping = new ResourceMapping(
        findRel(YetAnotherPersonRepository.class),
        findPath(YetAnotherPersonRepository.class),
        findExported(YetAnotherPersonRepository.class)
    );

    // The rel attribute defaults to class name
    assertThat(mapping.getRel(), is("yetAnotherPerson"));
    assertThat(mapping.getPath(), is("people"));
    // The exported defaults to true
    assertThat(mapping.isExported(), is(true));
  }

  @Test
  public void shouldDetectPathAndRemoveLeadingSlashIfAnyOnMethod() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findByFirstName", String.class, Pageable.class);
    ResourceMapping mapping = new ResourceMapping(
        findRel(method),
        findPath(method),
        findExported(method)
    );

    // The rel attribute defaults to class name
    assertThat(mapping.getRel(), is("findByFirstName"));
    assertThat(mapping.getPath(), is("firstname"));
    // The exported defaults to true
    assertThat(mapping.isExported(), is(true));
  }

  @Test
  public void shouldReturnDefaultIfPathContainsOnlySlashTextOnMethod() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findByLastName", String.class, Pageable.class);
    ResourceMapping mapping = new ResourceMapping(
        findRel(method),
        findPath(method),
        findExported(method)
    );

    // The rel defaults to method name
    assertThat(mapping.getRel(), is("findByLastName"));
    // The path contains only a leading slash therefore defaults to method name
    assertThat(mapping.getPath(), is("findByLastName"));
    // The exported defaults to true
    assertThat(mapping.isExported(), is(true));
  }

  @Test
  public void shouldNotExportMethodMarkedAsNonExportable() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findByFirstNameLike", String.class);
    assertThat(ResourceMappingUtils.findExported(method), is(false));
  }

  @Test
  public void shouldExportMethodWithAnnotatedSingleEntityArgument() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findByCreatedAfter", Date.class);
    assertThat(ResourceMappingUtils.findExported(method), is(true));
  }

  @Test
  public void shouldNotExportMethodWithNotAnnotatedSingleEntityArgument() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findByCreatedBefore", Date.class);
    assertThat(ResourceMappingUtils.findExported(method), is(false));
  }

  @Test
  public void shouldExportMethodWithOnlyPagingArgument() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findAll", Pageable.class);
    assertThat(ResourceMappingUtils.findExported(method), is(true));
  }

  @Test
  public void shouldExportMethodWithOnlySortingArgument() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod("findAll", Sort.class);
    assertThat(ResourceMappingUtils.findExported(method), is(true));
  }

  @Test
  public void shouldNotExportMethodWithAtLeastOneNonAnnotatedEntityParameter() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod(
        "findByCreatedAfterAndLastName",
        Date.class, String.class, Sort.class
    );
    assertThat(ResourceMappingUtils.findExported(method), is(false));
  }

  @Test
  public void shouldExportMethodWithAllAnnotatedEntityParameter() throws Exception {
    Method method = YetAnotherPersonRepository.class.getMethod(
        "findByCreatedAfterAndFirstName",
        Date.class, String.class, Pageable.class
    );
    assertThat(ResourceMappingUtils.findExported(method), is(true));
  }

}
