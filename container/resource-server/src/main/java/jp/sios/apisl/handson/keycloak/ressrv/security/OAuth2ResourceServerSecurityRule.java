/*
 * Copyright 2024 Toshiharu Konuma at SIOS Technology, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.sios.apisl.handson.keycloak.ressrv.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * OAuth authorization rule.
 *
 * - Please write your own rules for your application in this file.
 *
 * @author Toshiharu Konuma
 */
@Component
public class OAuth2ResourceServerSecurityRule {

	// {{{ public apply(HttpSecurity http) throws Exception
	public HttpSecurity apply(HttpSecurity http) throws Exception
	{
//		http
//			.headers()
//				.frameOptions()
//					.sameOrigin();

		return http;
	}
	// }}}

	public Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
			.AuthorizationManagerRequestMatcherRegistry> authorize() {
		return (authorize) -> authorize

			.requestMatchers("/").permitAll()
			.requestMatchers("/apis").authenticated()

			.requestMatchers("/apis/sports").hasAnyAuthority("SCOPE_baseball.player", "SCOPE_soccer.player")

			.requestMatchers("/apis/sports/baseball").hasAuthority("SCOPE_baseball.player")
			.requestMatchers("/apis/sports/baseball/managers").hasAuthority("SCOPE_baseball.manager")
			.requestMatchers("/apis/sports/baseball/players").hasAuthority("SCOPE_baseball.player")

			.requestMatchers("/apis/sports/soccer").hasAuthority("SCOPE_soccer.player")
			.requestMatchers("/apis/sports/soccer/managers").hasAuthority("SCOPE_soccer.manager")
			.requestMatchers("/apis/sports/soccer/players").hasAuthority("SCOPE_soccer.player")

			.requestMatchers("/happy").permitAll()
			.requestMatchers("/test1").authenticated()
			.requestMatchers("/test2").hasAuthority("SCOPE_my-scope-prd")
			.requestMatchers("/test3").hasAuthority("SCOPE_my-scope-dev")
			.requestMatchers("/test4").hasRole("my-role-admin")
			.requestMatchers("/test5").hasRole("my-role-staff")
			.requestMatchers("/test6").hasAuthority("ROLE_my-role-admin")

			.anyRequest().authenticated();
	}

}
