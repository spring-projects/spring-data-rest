/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.cassandra;

import java.io.IOException;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.BeforeClass;
import org.springframework.data.rest.webmvc.CommonWebTests;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/**
 * Base class for testing with an embedded cassandra database
 *
 * @author Greg Turnquist
 */
public abstract class AbstractCassandraIntegrationTest extends CommonWebTests {

	/**
	 * The session connected to the system keyspace.
	 */
	Session systemSession;

	/**
	 * The {@link com.datastax.driver.core.Cluster} that's connected to Cassandra.
	 */
	Cluster cluster;

	/**
	 * Launch an embedded Cassandra instance
	 *
	 * @throws ConfigurationException
	 * @throws IOException
	 * @throws TTransportException
	 */
	@BeforeClass
	public static void startDatabase() throws ConfigurationException, IOException, TTransportException {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra.yaml");
	}

	public AbstractCassandraIntegrationTest() {

		// check cluster
		if (cluster == null) {
			cluster = Cluster.builder()//
					.addContactPoint(CassandraProperties.HOSTNAME)//
					.withPort(CassandraProperties.PORT)//
					.build();
		}

		// check system session connected
		if (systemSession == null) {
			systemSession = tryToConnect(cluster, 3);
		}
	}

	private static Session tryToConnect(Cluster cluster, int attempts) {

		try {
			return cluster.connect();
		} catch (NoHostAvailableException o_O) {

			if (attempts == 0) {
				throw o_O;
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			return tryToConnect(cluster, --attempts);
		}
	}
}
