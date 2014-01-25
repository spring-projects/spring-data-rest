/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author Nick Weedon
 */
@Entity
public class Author {

	@Id @GeneratedValue//
	Long id;
	String name;

	@ManyToMany(mappedBy = "authors")//
	Set<Book> books = new HashSet<Book>();

	protected Author() {}

	public Set<Book> getBooks() {
		return books;
	}

	public void setBooks(Set<Book> books) {
		this.books = books;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Author(String name) {
		this.name = name;
	}

	public Author(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	// Simple 'shallow' hash for unit testing
	@Override
	public int hashCode() {
	     return new HashCodeBuilder(13, 31).
	       append(name).
	       append(id).
	       toHashCode();
	}

	// Simple 'shallow' compare for unit testing
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) {
			return false;
		}
		Author that = (Author) obj;
		return new EqualsBuilder()
			.append(name, that.name)
			.append(id, that.id)
			.isEquals();
	}
	
	
}
