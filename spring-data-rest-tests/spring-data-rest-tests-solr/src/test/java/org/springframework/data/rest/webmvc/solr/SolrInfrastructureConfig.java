/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.solr;

import static org.springframework.data.rest.webmvc.solr.TestUtils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.EmbeddedSolrServerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
@Configuration
public class SolrInfrastructureConfig {

	private static final String CORE_PROPERTIES = "name=collection1";
	private static final Resource MANAGED_SCHEMA = new ClassPathResource("managed-schema", SolrInfrastructureConfig.class);
	private static final Resource SOLR_CONFIG = new ClassPathResource("solrconfig.xml", SolrInfrastructureConfig.class);
	private static final Resource SOLR_SCHEMA = new ClassPathResource("schema.xml", SolrInfrastructureConfig.class);
	private static final Resource SOLR_XML = new ClassPathResource("solr.xml", SolrInfrastructureConfig.class);

	@Bean
	public SolrClientFactory solrClientFactory(final String solrHomeDir)
			throws ParserConfigurationException, IOException, SAXException {

		prepareConfiguration(solrHomeDir);
		return new EmbeddedSolrServerFactory(solrHomeDir);
	}

	@Bean
	public SolrTemplate solrTemplate(SolrClientFactory factory) {

		attachCloseHook(factory);
		return new SolrTemplate(factory);
	}

	private static void prepareConfiguration(final String solrHomePath) throws IOException {

		Map<String, String> configParams = new HashMap<String, String>();
		configParams.put("${data.dir}", solrHomePath);
		configParams.put("${lucene.version}", "5.3.1");

		Resource solrConfig = filterResource(SOLR_CONFIG, configParams);
		Resource solrSchema = SOLR_SCHEMA;
		Resource solrXml = SOLR_XML;
		Resource solrManagedSchema = MANAGED_SCHEMA;

		File solrHomeDir = new File(solrHomePath);
		File collectionDir = new File(solrHomeDir, "collection1");
		File confDir = new File(collectionDir, "conf");
		confDir.mkdirs();

		FileCopyUtils.copy(solrXml.getInputStream(), new FileOutputStream(createFile(solrHomeDir, "solr.xml")));
		FileCopyUtils.copy(CORE_PROPERTIES.getBytes(), new FileOutputStream(createFile(collectionDir, "core.properties")));
		FileCopyUtils.copy(solrSchema.getInputStream(), new FileOutputStream(createFile(confDir, "schema.xml")));
		FileCopyUtils.copy(solrManagedSchema.getInputStream(), new FileOutputStream(createFile(confDir, "managed-schema")));
		FileCopyUtils.copy(solrConfig.getInputStream(), new FileOutputStream(createFile(confDir, "solrconfig.xml")));
	}

	private static File createFile(File parent, String child) throws IOException {

		File file = new File(parent, child);
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}

	/**
	 * {@link SpringJUnit4ClassRunner} executes {@link ClassRule}s before the actual shutdown of the
	 * {@link ApplicationContext}. This causes the {@link TemporaryFolder} to vanish before Solr can gracefully shutdown.
	 * <br />
	 * To prevent error messages popping up we register a {@link CloseHook} re adding the index directory and removing it
	 * after {@link SolrCore#close()}.
	 * 
	 * @param factory
	 */
	private void attachCloseHook(SolrClientFactory factory) {

		EmbeddedSolrServer server = (EmbeddedSolrServer) factory.getSolrClient();

		for (SolrCore core : server.getCoreContainer().getCores()) {

			core.addCloseHook(new CloseHook() {

				private String path;

				@Override
				public void preClose(SolrCore core) {

					CoreDescriptor cd = core.getCoreDescriptor();

					if (cd == null) {
						return;
					}

					File tmp = new File(core.getIndexDir()).getParentFile();

					if (tmp.exists()) {
						return;
					}

					try {

						File indexFile = new File(tmp, "index");
						indexFile.mkdirs();

						this.path = indexFile.getPath();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void postClose(SolrCore core) {

					if (!StringUtils.hasText(this.path)) {
						return;
					}

					File tmp = new File(this.path);

					if (tmp.exists() && tmp.getPath().startsWith(FileUtils.getTempDirectoryPath())) {

						try {
							FileUtils.deleteDirectory(tmp);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
	}
}
