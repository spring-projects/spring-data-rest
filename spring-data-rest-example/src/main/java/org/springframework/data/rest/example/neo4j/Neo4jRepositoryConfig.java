package org.springframework.data.rest.example.neo4j;

import java.io.File;
import java.io.IOException;

import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.util.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.JtaTransactionManagerFactoryBean;
import org.springframework.data.neo4j.support.Neo4jExceptionTranslator;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Jon Brisbin
 */
@Configuration
@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = "org.springframework.data.rest.example.neo4j")
public class Neo4jRepositoryConfig {

	static final String DB_PATH = "db/neo4j-test-db";

	@Bean(destroyMethod = "shutdown") public EmbeddedGraphDatabase graphDatabaseService() throws IOException {
		FileUtils.deleteRecursively(new File(DB_PATH));
		return new EmbeddedGraphDatabase(DB_PATH);
	}

	@Bean public Neo4jTemplate neo4jTemplate() throws IOException {
		return new Neo4jTemplate(graphDatabaseService());
	}

	@Bean public Neo4jMappingContext neo4jMappingContext() {
		return new Neo4jMappingContext();
	}

	@Bean public JtaTransactionManagerFactoryBean transactionManager() throws Exception {
		return new JtaTransactionManagerFactoryBean(graphDatabaseService());
	}

	@Bean public Neo4jExceptionTranslator exceptionTranslator() {
		return new Neo4jExceptionTranslator();
	}

	@Bean(initMethod = "loadData") public Neo4jLoader neo4jLoader() {
		return new Neo4jLoader();
	}

}
