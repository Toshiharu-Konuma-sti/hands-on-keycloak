package jp.sios.apisl.handson.keycloak.rp.openid.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class OAuth2LoginSecurityConfiguration {

	private final OAuth2LoginSecurityRule securityRule;

	// {{{ public OAuth2LoginSecurityConfiguration(OAuth2LoginSecurityRule securityRule)
	public OAuth2LoginSecurityConfiguration(OAuth2LoginSecurityRule securityRule)
	{
		this.securityRule = securityRule;
	}
	// }}}

	// {{{ public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
	{
		http = this.securityRule.apply(http);
		http
			.authorizeHttpRequests(this.securityRule.authorize())
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.oidcUserService(this.oidcUserService())
//					.userAuthoritiesMapper(this.userAuthoritiesMapper())
				)
			);

		return http.build();
	}
	// }}}


	// {{{ private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService()
	private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService()
	{
		final OidcUserService delegate = new OidcUserService();
		return (userRequest) -> {
			OidcUser oidcUser = delegate.loadUser(userRequest);
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());

			OAuth2AccessToken accessToken = userRequest.getAccessToken();
			String tokenValue =  accessToken.getTokenValue();
			ArrayList<String> roles = this.pickupRolesFromAcToken(tokenValue);
			for (String role : roles) {
				mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			}
			oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());

			return oidcUser;
		};
	}
	// }}}

	// {{{ private ArrayList<String> pickupRolesFromAcToken(String accessToken)
	private ArrayList<String> pickupRolesFromAcToken(String accessToken)
	{
		ArrayList<String> roles = new ArrayList<String>();

		String[] tokenPart = accessToken.split("\\.");
		if (tokenPart.length > 1) {

			String payload = tokenPart[1].replaceAll("-", "+").replaceAll("_", "/");
			Base64.Decoder decoder = Base64.getDecoder();
			String decoded = new String(decoder.decode(payload.getBytes(StandardCharsets.UTF_8)));

			try {
				ObjectMapper  objectMapper = new ObjectMapper();
				JsonNode json = objectMapper.readTree(decoded);
				if ((json.get("realm_access") != null) &&
				    (json.get("realm_access").get("roles") != null)) {
					Iterator<JsonNode> realmRole = json.get("realm_access").get("roles").elements();
					roles.addAll(this.convJsonToList(realmRole));
				}
				if ((json.get("resource_access") != null) &&
				    (json.get("resource_access").get("account") != null) &&
				    (json.get("resource_access").get("account").get("roles") != null)) {
					Iterator<JsonNode> resrcRole = json.get("resource_access").get("account").get("roles").elements();
					roles.addAll(this.convJsonToList(resrcRole));
				}
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

		return roles;
	}
	// }}}

	// {{{ private ArrayList<String> convJsonToList(Iterator<JsonNode> json)
	private ArrayList<String> convJsonToList(Iterator<JsonNode> json)
	{
		ArrayList<String> roles = new ArrayList<String>();
		while (json.hasNext()) {
			roles.add(json.next().textValue());
		}
		return roles;
	}
	// }}}


	// {{{ private GrantedAuthoritiesMapper userAuthoritiesMapper()
	private GrantedAuthoritiesMapper userAuthoritiesMapper()
	{
		return (authorities) -> {
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
			authorities.forEach(authority -> {
				if (OidcUserAuthority.class.isInstance(authority)) {
					OidcUserAuthority oidcUserAuthority = (OidcUserAuthority)authority;
					OidcIdToken idToken = oidcUserAuthority.getIdToken();
					OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();
					//
					// Map the claims found in idToken and/or userInfo
					// to one or more GrantedAuthority's and add it to mappedAuthorities
					//
				}
				if (OAuth2UserAuthority.class.isInstance(authority)) {
					OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority)authority;
					Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
					//
					// Map the attributes found in userAttributes
					// to one or more GrantedAuthority's and add it to mappedAuthorities
					//
				}
				if (GrantedAuthority.class.isInstance(authority)) {
					GrantedAuthority gauth = (GrantedAuthority)authority;
				}
			});
			return mappedAuthorities;
		};
	}
	// }}}

}
