package org.springframework.data.rest.example.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;

import org.springframework.data.rest.core.annotation.Description;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * An entity that represents a person.
 * 
 * @author Jon Brisbin
 */
@Entity
public class Person {

	private Long id;
	private @Description("A person's first name") String firstName;
	private @Description("A person's last name") @NotNull String lastName;
	private @Description("Timestamp this person object was created") Date created;
	private @Description("A person's siblings") @RestResource(exported = false) List<Person> siblings = Collections
			.emptyList();
	private Person father;

	public Person() {}

	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Person addSibling(Person p) {
		if (siblings == Collections.EMPTY_LIST) {
			siblings = new ArrayList<Person>();
		}
		siblings.add(p);
		return this;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	public List<Person> getSiblings() {
		return siblings;
	}

	public void setSiblings(List<Person> siblings) {
		this.siblings = siblings;
	}

	@ManyToOne
	public Person getFather() {
		return father;
	}

	public void setFather(Person father) {
		this.father = father;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {}

	@PrePersist
	private void prePersist() {
		this.created = Calendar.getInstance().getTime();
	}

	@Override
	public String toString() {
		return "Person{" + "id=" + id + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\''
				+ ", siblings=[" + siblings.size() + "]" + ", father=" + father + ", created=" + created + '}';
	}

}
