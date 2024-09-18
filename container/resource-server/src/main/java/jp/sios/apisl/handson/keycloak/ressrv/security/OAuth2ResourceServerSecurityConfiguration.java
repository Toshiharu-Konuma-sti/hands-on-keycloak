/*
 * Copyright 2002-2019 the original author or authors. (Keycloak Authors)
 * Copyright 2024      Toshiharu Konuma at SIOS Technology, Inc.
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
 *
 * The original source code and the changes from it.
 * - The url below is the original source code:
 *   https://github.com/keycloak/keycloak-quickstarts/blob/latest/spring/rest-authz-resource-server/src/main/java/org/keycloak/quickstart/OAuth2ResourceServerSecurityConfiguration.java
 * - 2024.09.02:
 *   Role-based authorization processing is no longer dependent on
 *   connecting to Keycloak server,
 *   and can be determined from the role information in the Access Token.
 */
package jp.sios.apisl.handson.keycloak.ressrv.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth resource configuration.
 *
 * @author Toshiharu Konuma
 */
@Configuration
@EnableWebSecurity
public class OAuth2ResourceServerSecurityConfiguration {

	@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
	String jwkSetUri;

	private final OAuth2ResourceServerSecurityRule securityRule;

	// {{{ public OAuth2ResourceServerSecurityConfiguration(OAuth2ResourceServerSecurityRule securityRule) {
	public OAuth2ResourceServerSecurityConfiguration(OAuth2ResourceServerSecurityRule securityRule) {
		this.securityRule = securityRule;
	}
	// }}}

	// {{{ public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http = this.securityRule.apply(http);
		http
			.authorizeHttpRequests(this.securityRule.authorize())
			.oauth2ResourceServer(
				(oauth2) -> oauth2
					.jwt(jwt -> jwt
						.jwtAuthenticationConverter(this.jwtAuthConv()))
//						.jwtAuthenticationConverter(customJwtAuthenticationConverter()))
			);
		return http.build();
	}
	// }}}

	// {{{ JwtDecoder jwtDecoder()
	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
	}
	// }}}

	// {{{ private JwtAuthenticationConverter jwtAuthConv()
	private JwtAuthenticationConverter jwtAuthConv() {
		DelegatingJwtGrantedAuthoritiesConverter converter
			= new DelegatingJwtGrantedAuthoritiesConverter(
			new JwtGrantedAuthoritiesConverter(),
			new Converter<Jwt, Collection<GrantedAuthority>>() {
				@Override
				public Collection<GrantedAuthority> convert(Jwt source) {
					Collection<GrantedAuthority> grantedAuthorities
						= new ArrayList<>();
					for (String role : getRoles(source)) {
						grantedAuthorities.add(
							new SimpleGrantedAuthority("ROLE_" + role));
					}
					return grantedAuthorities;
				}

				// {{{ private Collection<String> getRoles(Jwt jwt)
				@SuppressWarnings("unchecked")
				private Collection<String> getRoles(Jwt jwt) {

					Map<String, Object> claims = jwt.getClaims();
					ArrayList<String> roles = new ArrayList<String>();
					ArrayList<String> fields = new ArrayList<String>();

					// "realm_access" -> "roles"
					fields = new ArrayList<String>(
						Arrays.asList("realm_access", "roles"));
					roles.addAll(this.pickupRole(claims, fields));

					// "resource_access" -> "account" -> "roles"
					fields = new ArrayList<String>(
						Arrays.asList("resource_access", "account", "roles"));
					roles.addAll(this.pickupRole(claims, fields));

					return (Collection<String>) roles;
				}
				// }}}

				// {{{ private ArrayList<String> pickupRole(Map<String, Object> list, List<String> keys)
				private ArrayList<String> pickupRole(Map<String, Object> list, List<String> keys) {
					ArrayList<String> roles = new ArrayList<String>();
					for (int i = 0; i < keys.size(); i++) {
						Object value = list.get(keys.get(i));
						if (value == null) {
							break;
						}
						if (i < (keys.size() - 1)) {
							list = (Map<String, Object>) value;
						} else {
							roles = (ArrayList<String>) value;
						}
					}
					return roles;
				}
				// }}}

			}
		);

		JwtAuthenticationConverter jwtAuthConv
			= new JwtAuthenticationConverter();
		jwtAuthConv.setJwtGrantedAuthoritiesConverter(converter);
		return jwtAuthConv;
	}
	// }}}

	// {{{ public JwtAuthenticationConverter customJwtAuthenticationConverter()
	/**
	 * ref) https://medium.com/@alperkrtglu/spring-oauth2-with-keycloak-moving-from-scope-to-roles-34247f3ff78e
	 */
	@Bean
	public JwtAuthenticationConverter customJwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
		return converter;
	}
	// }}}

	// {{{ private final class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> 
	private final class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
		@Override
		public Collection<GrantedAuthority> convert(Jwt jwt) {
			var realmAccess = (Map<String, List<String>>) jwt.getClaim("realm_access");
			return realmAccess.get("roles").stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.collect(Collectors.toList());
		}
		@Override
		public <U> Converter<Jwt, U> andThen(Converter<? super Collection<GrantedAuthority>, ? extends U> after) {
			return Converter.super.andThen(after);
		}
	}
	// }}}

}
