package org.springframework.data.rest.repository.test;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Entity
public class Family {

  @Id
  @GeneratedValue
  private Long id;
  private String surname;
  @RestResource(exported = false)
  @OneToMany
  private List<Person> members;

  public Family() {
  }

  public Family(String surname) {
    this.surname = surname;
  }

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

  @Override public String toString() {
    return "Family{" +
        "id=" + id +
        ", surname='" + surname + '\'' +
        ", members=" + members +
        '}';
  }

}
