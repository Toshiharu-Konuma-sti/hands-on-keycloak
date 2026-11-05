package jp.sios.apisl.handson.keycloak.ressrv.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	// すべてのリクエストにアクセストークンを要求する。APIごとの許可条件は、各APIの@PreAuthorizeに書く
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, ApiErrorWriter errors) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
			.oauth2ResourceServer(oauth -> oauth
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				.authenticationEntryPoint(errors::unauthorized));
		return http.build();
	}

	// トークンの中身をSpring Securityの権限に変換する
	//   scope クレーム             -> SCOPE_xxx (標準の変換)
	//   realm_access.roles クレーム -> ROLE_xxx  (Keycloakのレルムロール。ここで追加している)
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		var scopeConverter = new JwtGrantedAuthoritiesConverter();
		var converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
			for (String role : realmRoles(jwt)) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			}
			return authorities;
		});
		return converter;
	}

	@SuppressWarnings("unchecked")
	private List<String> realmRoles(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaim("realm_access");
		if (realmAccess == null || realmAccess.get("roles") == null) {
			return List.of();
		}
		return (List<String>) realmAccess.get("roles");
	}
}
