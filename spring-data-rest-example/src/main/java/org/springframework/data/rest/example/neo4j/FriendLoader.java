package org.springframework.data.rest.example.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jon Brisbin
 */
public class FriendLoader {

	@Autowired
	private FriendRepository friends;

	@SuppressWarnings({"unchecked"})
	@Transactional
	public void loadFriends() {
		Friend bob = new Friend("Bob");
		Friend dean = new Friend("Dean");
		Friend jim = new Friend("Jim");

		Set<Friend> friends = new HashSet<Friend>();
		friends.add(dean);
		friends.add(jim);

		bob.setFriends(friends);

		this.friends.save(bob);

		System.out.println("Saved Bob: " + bob);
	}

}
