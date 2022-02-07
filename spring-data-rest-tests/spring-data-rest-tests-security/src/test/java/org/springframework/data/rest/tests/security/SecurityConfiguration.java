/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.data.rest.tests.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

// tag::code[]
@Configuration // <1>
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true) // <2>
class SecurityConfiguration extends WebSecurityConfigurerAdapter { // <3>
// end::code[]
	@Autowired
	void configureAuth(AuthenticationManagerBuilder auth) throws Exception {

		auth.inMemoryAuthentication()
			.withUser("user").password("user").roles("USER").and()
			.withUser("admin").password("admin").roles("USER", "ADMIN");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.
			authorizeRequests()
				.antMatchers(HttpMethod.GET, "/").permitAll() // Ignore security at the root URI.
				.anyRequest().authenticated()
				.and()
			.httpBasic()
				.and()
			.csrf().disable(); // Disable CSRF since it's not critical for the scope of testing.
	}
}
