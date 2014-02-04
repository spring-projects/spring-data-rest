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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author Nick Weedon
 */
@Entity
public class Book {

	@Id String isbn;

	@ManyToMany(cascade = { CascadeType.MERGE })//
	Set<Author> authors;

	String title;

	protected Book() {}

	public Book(String isbn, String title, Iterable<Author> authors) {

		this.isbn = isbn;
		this.title = title;

		this.authors = new HashSet<Author>();

		for (Author author : authors) {
			author.books.add(this);
			this.authors.add(author);
		}
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	// Simple 'shallow' hash for unit testing
	@Override
	public int hashCode() {
	     return new HashCodeBuilder(17, 13).
	       append(title).
	       append(isbn).
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
		Book that = (Book) obj;
		return new EqualsBuilder()
			.append(title, that.title)
			.append(isbn, that.isbn)
			.isEquals();
	}
}
