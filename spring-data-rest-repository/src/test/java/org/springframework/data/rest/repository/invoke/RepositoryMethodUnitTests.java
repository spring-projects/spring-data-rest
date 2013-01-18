package org.springframework.data.rest.repository.invoke;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.util.ReflectionUtils.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;
import org.springframework.data.rest.repository.support.Methods;
import org.springframework.util.ReflectionUtils;

/**
 * Tests to verify the integrity of the {@link RepositoryMethod} abstraction.
 *
 * @author Jon Brisbin
 */
public class RepositoryMethodUnitTests {

  Map<String, RepositoryMethod> methods = new HashMap<String, RepositoryMethod>();
  RepositoryMethod method;

  @Before
  public void setup() {
    doWithMethods(PersonRepository.class,
                  new ReflectionUtils.MethodCallback() {
                    @Override public void doWith(Method method) throws IllegalArgumentException,
                                                                       IllegalAccessException {
                      String name = method.getName();
                      RepositoryMethod repoMethod = new RepositoryMethod(method);
                      methods.put(name, repoMethod);
                    }
                  },
                  Methods.USER_METHODS);
    method = methods.get("findByFirstName");
  }

  @Test
  public void shouldFindSimpleQueryMethods() throws Exception {
    assertThat(method, notNullValue());
  }

  @Test
  public void shouldFindPageableInformationOnMethod() throws Exception {
    assertThat(method, notNullValue());
    assertThat(method.isPageable(), is(true));
  }

  @Test
  public void shouldNotFindSortInformationOnMethod() throws Exception {
    assertThat(method, notNullValue());
    assertThat(method.isSortable(), is(false));
  }

  @Test
  public void shouldProvideParameterClassTypes() throws Exception {
    assertThat(method, notNullValue());
    assertThat(method.getParameters().get(0).getParameterType(), is(typeCompatibleWith(String.class)));
    assertThat(method.getParameters().get(1).getParameterType(), is(typeCompatibleWith(Pageable.class)));
  }

  @Test
  public void shouldProvideParameterNames() throws Exception {
    assertThat(method, notNullValue());
    assertThat(method.getParameterNames(), contains("firstName", "arg1"));
  }

}
