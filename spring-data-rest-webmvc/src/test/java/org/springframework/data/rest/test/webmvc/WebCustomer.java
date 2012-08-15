package org.springframework.data.rest.test.webmvc;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * @author Jon Brisbin
 */
@Entity
public class WebCustomer extends Customer {

  private String               username;
  @OneToMany
  private Map<Profile, Person> people;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Map<Profile, Person> getPeople() {
    return people;
  }

  public void setPeople(Map<Profile, Person> people) {
    this.people = people;
  }

}
