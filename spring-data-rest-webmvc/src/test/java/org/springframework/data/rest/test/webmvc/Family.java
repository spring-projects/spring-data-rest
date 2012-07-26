package org.springframework.data.rest.test.webmvc;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@Entity
public class Family {

  @Id @GeneratedValue private Long         id;
  private                     String       surname;
  @OneToMany
  private                     List<Person> members;

  public Long getId() {
    return id;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public List<Person> getMembers() {
    return members;
  }

  public void setMembers(List<Person> members) {
    this.members = members;
  }

}
