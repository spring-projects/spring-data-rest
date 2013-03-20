package org.springframework.data.rest.example.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jon Brisbin
 */
public class Neo4jLoader {

	private static final Logger LOG = LoggerFactory.getLogger(Neo4jLoader.class);
	@Autowired
	private FriendRepository friends;
	@Autowired
	private MovieRepository  movies;
	@Autowired
	private StudioRepository studios;

	@SuppressWarnings({"unchecked"})
	@Transactional
	public void loadData() {
		Friend bob = new Friend("Bob");
		Friend dean = new Friend("Dean");
		Friend jim = new Friend("Jim");

		Set<Friend> friends = new HashSet<Friend>();
		friends.add(dean);
		friends.add(jim);

		bob.setFriends(friends);

		this.friends.save(bob);

		LOG.info("Loaded {}", bob);

		Studio studio = studios.save(new Studio("MGM"));
		Movie wizardOfOz = movies.save(new Movie("The Wizard of Oz", studio));

		LOG.info("Loaded {} and {}", studio, wizardOfOz);
	}

}
