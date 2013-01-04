package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
 */
@Component
public class PersonLoader implements InitializingBean {

  @Autowired
  private PlainPersonRepository people;

  @Override public void afterPropertiesSet() throws Exception {
    people.save(new Person("John", "Doe"));
  }

}
