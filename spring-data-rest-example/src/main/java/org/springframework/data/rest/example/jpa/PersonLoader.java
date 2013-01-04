package org.springframework.data.rest.example.jpa;

import java.util.Arrays;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
 */
@Component
public class PersonLoader implements InitializingBean {

  @Autowired
  PersonRepository people;

  @Override public void afterPropertiesSet() throws Exception {
    Person billyBob = people.save(new Person("Billy Bob", "Thornton"));

    Person john = new Person("John", "Doe");
    Person jane = new Person("Jane", "Doe");
    john.addSibling(jane);
    john.setFather(billyBob);
    jane.addSibling(john);
    jane.setFather(billyBob);

    people.save(Arrays.asList(john, jane));
  }

}
