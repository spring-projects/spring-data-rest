package org.springframework.data.rest.test.webmvc;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

/**
 * @author Jon Brisbin
 */
@MappedSuperclass
public class Customer {

  @Id @GeneratedValue private Long   id;
  @OneToOne private           Person person;

  public Long getId() {
    return id;
  }

  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

}
