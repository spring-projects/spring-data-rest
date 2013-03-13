package org.springframework.data.rest.example.neo4j;

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author Jon Brisbin
 */
@NodeEntity
public class Friend {

	@GraphId
	private Long        id;
	private String      name;
	@RelatedTo(type = "BFF", direction = Direction.INCOMING)
	private Set<Friend> friends;

	protected Friend() {
	}

	public Friend(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Friend> getFriends() {
		return friends;
	}

	public void setFriends(Set<Friend> friends) {
		this.friends = friends;
	}

	@Override public String toString() {
		return "Friend{" +
				"id=" + id +
				", name='" + name + '\'' +
				", friends=[" + (null != friends ? friends.size() : 0) + "]" +
				'}';
	}

}
