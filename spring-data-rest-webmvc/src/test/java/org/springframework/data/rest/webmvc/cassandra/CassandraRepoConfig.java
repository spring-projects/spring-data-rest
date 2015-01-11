package org.springframework.data.rest.webmvc.cassandra;

import static org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification.*;

import java.util.Arrays;
import java.util.List;

import org.springframework.cassandra.core.keyspace.CreateKeyspaceSpecification;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * Basic configuration for a Cassandra set up.
 *
 * @author Greg Turnquist
 */
@Configuration
@EnableCassandraRepositories
public class CassandraRepoConfig extends AbstractCassandraConfiguration {

	@Override
	protected String getKeyspaceName() {
		return "SpringDataRestCassandra";
	}

	@Override
	public SchemaAction getSchemaAction() {
		return SchemaAction.RECREATE_DROP_UNUSED;
	}

	@Override
	protected int getPort() {
		return CassandraProperties.PORT;
	}

	@Override
	public String[] getEntityBasePackages() {
		return new String[] {this.getClass().getPackage().getName()};
	}

	/**
	 * When retrieving the keyspaces, create one for testing.
	 *
	 * @return
	 */
	@Override
	protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
		return Arrays.asList(createKeyspace().name(getKeyspaceName()).withSimpleReplication());
	}
}
