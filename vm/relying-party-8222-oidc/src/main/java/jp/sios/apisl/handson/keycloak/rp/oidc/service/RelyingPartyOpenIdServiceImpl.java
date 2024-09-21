package jp.sios.apisl.handson.keycloak.rp.oidc.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

@Service
public class RelyingPartyOpenIdServiceImpl implements RelyingPartyOpenIdService {

	private final OAuth2AuthorizedClientService authorizedClientService;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${handson.relying-party.api-server.host}")
	String apiHost;

	// {{{ public RelyingPartyOpenIdServiceImpl(OAuth2AuthorizedClientService authorizedClientService)
	public RelyingPartyOpenIdServiceImpl(OAuth2AuthorizedClientService authorizedClientService)
	{
		this.authorizedClientService = authorizedClientService;
	}
	// }}}

	// {{{ public String webapp(OAuth2AuthenticationToken token, ModelAndView model)
	@Override
	public String webapp(OAuth2AuthenticationToken token, ModelAndView model)
	{
		String viewName = "index";
		OAuth2AuthorizedClient authorizedClient = this.loadAuthClientFromService(token);
		if (authorizedClient == null) {
			viewName = this.makeLoginRedirectUrl(token);
		} else {
			OAuth2AccessToken accessToken = this.extractAccessToken(authorizedClient);
			OAuth2RefreshToken refreshToken = this.extractRefreshToken(authorizedClient);
			OidcIdToken idToken = this.extractIdToken(token);
			OAuth2AccessToken.TokenType acTokenType = accessToken.getTokenType();
			List<String> acTokenRoles = this.extractRoles(token);
			List<String> acTokenScopes = this.extractScopes(token);

			model.addObject("access_token", accessToken.getTokenValue());
			model.addObject("acTokenIssuedAt", accessToken.getIssuedAt());
			model.addObject("acTokenExpiresAt", accessToken.getExpiresAt());
			model.addObject("acTokenType", acTokenType.getValue());
			model.addObject("refresh_token", refreshToken.getTokenValue());
			model.addObject("id_token", idToken.getTokenValue());
			model.addObject("id_token", idToken.getTokenValue());

			model.addObject("acTokenRoles", acTokenRoles);
			model.addObject("acTokenScopes", acTokenScopes);

		}
		this.printToken(token);

		return viewName;
	}
	// }}}

	// {{{ public String refresh(OAuth2AuthenticationToken token)
	@Override
	public String refresh(OAuth2AuthenticationToken token)
	{
		String viewName = "redirect:/";
		OAuth2AuthorizedClient authorizedClient = this.loadAuthClientFromService(token);
		if (authorizedClient == null) {
			viewName = this.makeLoginRedirectUrl(token);
		} else {
			try {
				OAuth2AuthorizedClient newAuthorizedClient = this.refreshAuthorizedClient(authorizedClient);
				this.removeAuthClientFromService(token);
				this.saveAuthClientToService(newAuthorizedClient, token);
			} catch (OAuth2AuthorizationException e) {
				viewName = this.makeLoginRedirectUrl(token);
			}
		}

		return viewName;
	}
	// }}}

	// {{{ public String logout(OAuth2AuthenticationToken token)
	@Override
	public String logout(OAuth2AuthenticationToken token)
	{
		OAuth2AuthorizedClient authorizedClient = this.loadAuthClientFromService(token);
		String logoutUrl = this.getEndSessionEndpoint(authorizedClient);
		String viewName = String.format("redirect:%s", logoutUrl);
		this.removeAuthClientFromService(token);

		return viewName;
	}
	// }}}

	// {{{ public String broker(OAuth2AuthenticationToken token, ModelAndView model, String apiPath)
	@Override
	public String broker(OAuth2AuthenticationToken token, ModelAndView model, String apiPath)
	{
		String viewName = "broker";
		String apiResp = "(no response)";
		if ((apiPath != null) && (apiPath.isEmpty() == false)) {
			OAuth2AuthorizedClient authorizedClient = this.loadAuthClientFromService(token);
			String apiUrl = this.makeApiUrl(apiPath, authorizedClient);
			OAuth2AccessToken accessToken = this.extractAccessToken(authorizedClient);
			HttpResponse<String> httpResp = this.sendApiServer(apiUrl, accessToken);
			HttpHeaders httpHead = httpResp.headers();

			apiResp = this.fetchHttpStatCodeLine(httpResp);
			apiResp += this.fetchHttpHeaderLines(httpHead);
			apiResp += "\n\n" + httpResp.body();
		}
		model.addObject("api_resp", apiResp);

		return viewName;
	}
	// }}}


	// {{{ private OAuth2AuthorizedClient loadAuthClientFromService(OAuth2AuthenticationToken token)
	private OAuth2AuthorizedClient loadAuthClientFromService(OAuth2AuthenticationToken token)
	{
		OAuth2AuthorizedClient authorizedClient =
		this.authorizedClientService.loadAuthorizedClient(
			token.getAuthorizedClientRegistrationId(),
			token.getName()
		);
		return authorizedClient;
	}
	// }}}

	// {{{ private String makeLoginRedirectUrl(OAuth2AuthenticationToken token)
	private String makeLoginRedirectUrl(OAuth2AuthenticationToken token)
	{
		String path = String.format("redirect:%s/%s",
			OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI,
			token.getAuthorizedClientRegistrationId());
		return path;
	}
	// }}}

	// {{{ private OAuth2AccessToken extractAccessToken(OAuth2AuthorizedClient authorizedClient)
	private OAuth2AccessToken extractAccessToken(OAuth2AuthorizedClient authorizedClient)
	{
		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		return accessToken;
	}
	// }}}

	// {{{ private OAuth2RefreshToken extractRefreshToken(OAuth2AuthorizedClient authorizedClient)
	private OAuth2RefreshToken extractRefreshToken(OAuth2AuthorizedClient authorizedClient)
	{
		OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
		return refreshToken;
	}
	// }}}

	// {{{ private OidcIdToken extractIdToken(OAuth2AuthenticationToken token)
	private OidcIdToken extractIdToken(OAuth2AuthenticationToken token)
	{
		OAuth2User principal = token.getPrincipal(); 
		DefaultOidcUser oidcUser = (DefaultOidcUser)principal;
		OidcIdToken idToken = oidcUser.getIdToken(); 
		return idToken;
	}
	// }}}

	// {{{ private List<String> extractRoles(OAuth2AuthenticationToken token)
	private List<String> extractRoles(OAuth2AuthenticationToken token)
	{
		List<String> roles = this.extractFromAthorities(token, "ROLE_");
		return roles;
	}
	// }}}

	// {{{ private List<String> extractScopes(OAuth2AuthenticationToken token)
	private List<String> extractScopes(OAuth2AuthenticationToken token)
	{
		List<String> scopes = this.extractFromAthorities(token, "SCOPE_");
		return scopes;
	}
	// }}}

	// {{{ private String extractFromAthorities(OAuth2AuthenticationToken token, String prefix)
	private List<String> extractFromAthorities(OAuth2AuthenticationToken token, String prefix)
	{
		List<String> list = new ArrayList<String>();
		Collection<GrantedAuthority> authorities = token.getAuthorities();
		for (GrantedAuthority garantedAuth : authorities) {
			String authority = garantedAuth.getAuthority();
			if (authority.startsWith(prefix) == true) {
				list.add(authority.substring(prefix.length()));
			}
		}
		return list;
	}
	// }}}

	// {{{ private OAuth2AuthorizedClient refreshAuthorizedClient(OAuth2AuthorizedClient authorizedClient)
	private OAuth2AuthorizedClient refreshAuthorizedClient(OAuth2AuthorizedClient authorizedClient)
	{
		// get new access token from refresh token.
		DefaultRefreshTokenTokenResponseClient tokenResponseClient = new DefaultRefreshTokenTokenResponseClient();
		OAuth2RefreshTokenGrantRequest tokenRequest =
			new OAuth2RefreshTokenGrantRequest(
				authorizedClient.getClientRegistration(),
				authorizedClient.getAccessToken(),
				authorizedClient.getRefreshToken()
			);
		OAuth2AccessTokenResponse tokenResponse =
			tokenResponseClient.getTokenResponse(tokenRequest);

		// create new authorized client with new access token.
		OAuth2AuthorizedClient newAuthorizedClient = new OAuth2AuthorizedClient(
			authorizedClient.getClientRegistration(),
			authorizedClient.getPrincipalName(),
			tokenResponse.getAccessToken(),
			tokenResponse.getRefreshToken()
		);

		return newAuthorizedClient;
	}
	// }}}

	// {{{ private void removeAuthClientFromService(OAuth2AuthenticationToken token)
	private void removeAuthClientFromService(OAuth2AuthenticationToken token)
	{
		this.authorizedClientService.removeAuthorizedClient(
			token.getAuthorizedClientRegistrationId(),
			token.getName()
		);
	}
	// }}}

	// {{{ private void saveAuthClientToService(OAuth2AuthorizedClient newAuthorizedClient, OAuth2AuthenticationToken token)
	private void saveAuthClientToService(OAuth2AuthorizedClient newAuthorizedClient, OAuth2AuthenticationToken token)
	{
		this.authorizedClientService.saveAuthorizedClient(
			newAuthorizedClient,
			token
		);
	}
	// }}}

	// {{{ private String getEndSessionEndpoint(OAuth2AuthorizedClient authorizedClient)
	private String getEndSessionEndpoint(OAuth2AuthorizedClient authorizedClient)
	{
		String url = this.getConfigMetadataItem(authorizedClient, "end_session_endpoint");
		return url;
	}
	// }}}

	// {{{ private String getUserInfoEndpoint(OAuth2AuthorizedClient authorizedClient)
	private String getUserInfoEndpoint(OAuth2AuthorizedClient authorizedClient)
	{
		String url = this.getConfigMetadataItem(authorizedClient, "userinfo_endpoint");
		return url;
	}
	// }}}

	// {{{ private String getConfigMetadataItem(OAuth2AuthorizedClient authorizedClient, String key)
	private String getConfigMetadataItem(OAuth2AuthorizedClient authorizedClient, String key)
	{
		Map<String, Object> meta =
			authorizedClient.
				getClientRegistration().
				getProviderDetails().
				getConfigurationMetadata();
		String value = (String) meta.get(key);

		return value;
	}
	// }}}

	// {{{ private String makeApiUrl(String apiPath)
	private String makeApiUrl(String apiPath, OAuth2AuthorizedClient authorizedClient)
	{
		String url = this.apiHost + apiPath;
		if (apiPath.startsWith("/userinfo") == true) {
			url = this.getUserInfoEndpoint(authorizedClient);
		}
		return url;
	}
	// }}}

	// {{{ private HttpResponse<String> sendApiServer(String apiUrl, OAuth2AccessToken accessToken)
	private HttpResponse<String> sendApiServer(String apiUrl, OAuth2AccessToken accessToken)
	{
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl))
			.GET();
		if (accessToken != null) {
			builder.setHeader("Authorization", "Bearer " + accessToken.getTokenValue());
		}
		HttpRequest httpReq = builder.build();

		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> httpResp = null;
		try {
			httpResp = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException ex) {
			ex.printStackTrace();
		}

		return httpResp;
	}
	// }}}

	// {{{ private String fetchHttpStatCodeLine(HttpResponse<String> httpResp)
	private String fetchHttpStatCodeLine(HttpResponse<String> httpResp)
	{
		String line = "HTTP/x.x";
		if (httpResp.version().equals(HttpClient.Version.HTTP_1_1)) {
			line = "HTTP/1.1";
		} else
		if (httpResp.version().equals(HttpClient.Version.HTTP_2)) {
			line = "HTTP/2";
		}
		line += " " + httpResp.statusCode();

		return line;
	}
	// }}}

	// {{{ private String fetchHttpHeaderLines(HttpHeaders httpHead)
	private String fetchHttpHeaderLines(HttpHeaders httpHead)
	{
		String line = "";
		Map<String, List<String>> headLines = httpHead.map();
		for (String headName : headLines.keySet()) {
			if (headName.startsWith(":") == true) {
				continue;
			}
			List<String> vals = headLines.get(headName);
			for (String val : vals) {
				line += String.format("\n%s: %s", headName, val);
			}
		}

		return line;
	}
	// }}}


	// {{{ private void log(String message)
	private void log(String message)
	{
//		logger.error(message);
		logger.debug(message);
	}
	// }}}

	// {{{ private void printToken(OAuth2AuthenticationToken token)
	private void printToken(OAuth2AuthenticationToken token)
	{
		OAuth2User principal = token.getPrincipal();
		OAuth2AuthorizedClient authorizedClient = this.loadAuthClientFromService(token);

		String logMessage = "\n"
			+ "############### STRART: the token information ###############\n"
			+ "### 0 [" + token + "]\n"
			+ "### 1 [" + token.getName() + "]\n"
			+ "### 2 [" + token.getAuthorities() + "]\n"
			+ "### 3 [" + token.getAuthorizedClientRegistrationId() + "]\n"
			+ "### 4 [" + token.getCredentials() + "]\n"
			+ "### 5 [" + principal + "]\n"
			+ "### 6 [" + principal.getAttributes() + "]\n"
			+ "### 7 [" + (authorizedClient == null ? "no token" : "having token") + "]";

		if (authorizedClient != null) {
			OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
			OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
			OAuth2AccessToken.TokenType actType = accessToken.getTokenType();
			logMessage += "\n"
				+ "### 8 [" + authorizedClient.getPrincipalName() + "]\n"
				+ "### AcToken [" + accessToken.getTokenValue() + "]\n"
				+ "### RfToken [" + refreshToken.getTokenValue() + "]\n"
				+ "### AcT(1 [" + accessToken.getScopes() + "]\n"
				+ "### AcT(2 [" + accessToken.getExpiresAt() + "]\n"
				+ "### AcT(3 [" + accessToken.getIssuedAt() + "]\n"
				+ "### AcT(4 [" + actType.getValue() + "]\n"
				+ "### RfT(1 [" + refreshToken.getExpiresAt() + "]\n"
				+ "### RfT(2 [" + refreshToken.getIssuedAt() + "]";

			Map<String, Object> meta = 
				authorizedClient.
					getClientRegistration().
					getProviderDetails().
					getConfigurationMetadata();
			logMessage += "\n"
				+ "### meta [" + meta + "]";


		}
		if (DefaultOidcUser.class.isInstance(principal)) {
			DefaultOidcUser oidcUser = (DefaultOidcUser) principal;
			OidcIdToken idToken = oidcUser.getIdToken();
			logMessage += "\n"
				+ "### IdToken [" + idToken.getTokenValue() + "]\n"
				+ "### IdT(1 [" + idToken.getClaims() + "]\n"
				+ "### IdT(2 [" + idToken.getExpiresAt() + "]\n"
				+ "### IdT(3 [" + idToken.getIssuedAt() + "]";
		}
		if (DefaultOAuth2User.class.isInstance(principal)) {
			DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
			logMessage += "\n"
				+ "### 9 [" + oauth2User.getAttributes() + "]"
				+ "### A [" + oauth2User.getAuthorities() + "]\n";
		}

		this.log(logMessage);

	}
	// }}}

}








