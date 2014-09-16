/*
 * Copyright 2014 the original author or authors.
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

import static org.springframework.data.rest.webmvc.util.TestUtils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.data.solr.server.support.EmbeddedSolrServerFactory;
import org.springframework.util.FileCopyUtils;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
@Configuration
public class SolrInfrastructureConfig {

	private static final String CORE_PROPERTIES = "name=collection1";
	private static final Resource SOLR_CONFIG = new ClassPathResource("solrconfig.xml", SolrInfrastructureConfig.class);
	private static final Resource SOLR_SCHEMA = new ClassPathResource("schema.xml", SolrInfrastructureConfig.class);

	@Bean
	public SolrServerFactory solrServerFactory(final String solrHomeDir) throws ParserConfigurationException,
			IOException, SAXException {

		prepareConfiguration(solrHomeDir);
		return new EmbeddedSolrServerFactory(solrHomeDir);
	}

	@Bean
	public SolrTemplate solrTemplate(SolrServerFactory factory) {
		return new SolrTemplate(factory);
	}

	private void prepareConfiguration(final String solrHomeDir) throws IOException {

		Map<String, String> configParams = new HashMap<String, String>() {
			{
				put("${data.dir}", solrHomeDir);
				put("${lucene.version}", "4.7");
			}
		};

		Resource solrConfig = filterResource(SOLR_CONFIG, configParams);
		Resource solrSchema = SOLR_SCHEMA;

		File confDir = new File(new File(solrHomeDir, "collection1"), "conf");
		confDir.mkdirs();

		FileCopyUtils.copy(solrSchema.getInputStream(), new FileOutputStream(createFile(confDir, "schema.xml")));
		FileCopyUtils.copy(solrConfig.getInputStream(), new FileOutputStream(createFile(confDir, "solrconfig.xml")));
		FileCopyUtils.copy(CORE_PROPERTIES.getBytes(),
				new FileOutputStream(createFile(new File(solrHomeDir), "config.properties")));
	}

	private File createFile(File parent, String child) throws IOException {

		File file = new File(parent, child);
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}
}
