package jp.sios.apisl.handson.keycloak.rp.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import jp.sios.apisl.handson.keycloak.rp.oauth.entity.OidcConfigResponse;
import jp.sios.apisl.handson.keycloak.rp.oauth.entity.OidcTokenResponse;
import jp.sios.apisl.handson.keycloak.rp.oauth.repository.OidcConfigRepository;
import jp.sios.apisl.handson.keycloak.rp.oauth.repository.OidcTokenRepository;

/**
 *
 * Relying Party Service Implement Class
 *
 */
@Service
public class RelyingPartyServiceImpl implements RelyingPartyService
{

	private OidcConfigRepository oidcConfigRepository;
	private OidcTokenRepository oidcTokenRepository;

	private final static String CNF_PATH_CLI_SECRET = "handson.relying-party.openid.client.%s.secret";

	private final Environment environment;

	@Value("${handson.relying-party.api-server.host}")
	String apiHost;

	// {{{ public RelyingPartyServiceImpl(...)
	public RelyingPartyServiceImpl(
		OidcConfigRepository oidcConfigRepository,
		OidcTokenRepository oidcTokenRepository,
		Environment environment
	)
	{
		this.oidcConfigRepository = oidcConfigRepository;
		this.oidcTokenRepository = oidcTokenRepository;
		this.environment = environment;
	}
	// }}}

	// {{{ public ModelAndView webapp(
	@Override
	public ModelAndView webapp(
			HttpServletRequest request,
			ModelAndView model,
			String code,
			String auth_code,
			String access_token, String refresh_token, String id_token,
			String token_type, String expires_in
			)
	{
		String curUrl = request.getRequestURL().toString();
		String appType = curUrl.replaceAll(".*/", "");
		String clientId = "app-" + appType.replaceAll("-.*", "");
		String clientSecretPath = String.format(this.CNF_PATH_CLI_SECRET, clientId);
		String clientSecret = environment.getProperty(clientSecretPath);
		OidcConfigResponse respConfig = this.oidcConfigRepository.discovery();

		// refresh the access token.
		if (refresh_token != null) {
			String urlToken = respConfig.getToken_endpoint();
			OidcTokenResponse respToken = this.oidcTokenRepository.refresh(
				urlToken, clientId, clientSecret, refresh_token);
			this.packTokenValues(model,
				auth_code,
				respToken.getAccess_token(), respToken.getRefresh_token(),
				respToken.getId_token(),
				respToken.getToken_type(), respToken.getExpires_in());
		} else
		// retrieve the access token.
		if (code != null) {
			String urlToken = respConfig.getToken_endpoint();
			OidcTokenResponse respToken = this.oidcTokenRepository.generate(
				urlToken, clientId, clientSecret, code, curUrl);
			this.packTokenValues(model,
				code,
				respToken.getAccess_token(), respToken.getRefresh_token(),
				respToken.getId_token(),
				respToken.getToken_type(), respToken.getExpires_in());
		} else {
		// other processing (initial display, etc)
			this.packTokenValues(model,
				auth_code,
				access_token, refresh_token, id_token, token_type, expires_in);
		}

		this.packRoleAndScope(model);
		String authUrl = this.makeAuthUrl(respConfig, appType, clientId, curUrl);
		model.addObject("appType", appType);
		model.addObject("curUrl", curUrl);
		model.addObject("authUrl", authUrl);
		model.addObject("logoutUrl", respConfig.getEnd_session_endpoint());
		model.addObject("clientId", clientId);
		model.addObject("clientSecret", clientSecret);

		return model;
	}
	// }}}

	// {{{ private void packTokenValues(...)
	private void packTokenValues(
			ModelAndView model, String auth_code,
			String access_token, String refresh_token, String id_token,
			String token_type, String expires_in
		)
	{
		model.addObject("auth_code", auth_code);
		model.addObject("access_token", access_token);
		model.addObject("refresh_token", refresh_token);
		model.addObject("id_token", id_token);
		model.addObject("acTokenType", token_type);
		model.addObject("acTokenExpiresIn", expires_in);

		return;
	}
	// }}}

	// {{{ private void packRoleAndScope(ModelAndView model)
	private void packRoleAndScope(ModelAndView model)
	{
		List<String> roles = new ArrayList<>();
		List<String> scopes = new ArrayList<>();
		String ac_token = (String) model.getModel().get("access_token");

		if (ac_token != null) {
			String[] ac_part = ac_token.split("\\.");
			if (ac_part.length > 1) {

				String payload = ac_part[1].replaceAll("-", "+").replaceAll("_", "/");
				Base64.Decoder decoder = Base64.getDecoder();
				String decoded = new String(decoder.decode(payload.getBytes(StandardCharsets.UTF_8)));

				try {

					ObjectMapper  objectMapper = new ObjectMapper();
					JsonNode json = objectMapper.readTree(decoded);

					Iterator<JsonNode> realmRole = json.get("realm_access").get("roles").elements();
					Iterator<JsonNode> resrcRole = json.get("resource_access").get("account").get("roles").elements();
					roles = this.addRoleList(roles, realmRole);
					roles = this.addRoleList(roles, resrcRole);

					scopes = Arrays.asList(json.get("scope").textValue().split(" "));

//				} catch (IllegalArgumentException | JsonProcessingException e) {
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}
		}
		model.addObject("acTokenRoles", roles);
		model.addObject("acTokenScopes", scopes);

		return;
	}
	// }}}

	// {{{ private List<String> addRoleList(List<String> list, Iterator<JsonNode> roles)
	private List<String> addRoleList(List<String> list, Iterator<JsonNode> roles)
	{
		while (roles.hasNext()) {
			list.add(roles.next().textValue());
		}
		return list;
	}
	// }}}

	// {{{ private String makeAuthUrl(OidcConfigResponse resp, String appType, String clientId, String redirect)
	private String makeAuthUrl(OidcConfigResponse resp, String appType, String clientId, String redirect)
	{
		List<String> scopes = new ArrayList<String>(Arrays.asList("openid"));
		if (appType.endsWith("-manager")) {
			scopes.add(appType.replaceAll("-.*", "") + ".manager");
		}
		String url = resp.getAuthorization_endpoint()
			+ "?scope=" + String.join("+", scopes)
			+ "&response_type=code"
			+ "&client_id=" + clientId
			+ "&redirect_uri=" + redirect;

		return url;
	}
	// }}}

	// {{{ public ModelAndView broker(ModelAndView mav, String apiPath, String access_token)
	@Override
	public ModelAndView broker(ModelAndView mav, String apiPath, String access_token)
	{

		String apiResp = "(no response)";

		if ((apiPath != null) && (apiPath.isEmpty() == false)) {

			String apiUrl = this.makeSendUrl(apiPath);
			HttpResponse<String> httpResp = this.sendApiServer(apiUrl, access_token);
			HttpHeaders httpHead = httpResp.headers();

			apiResp = this.fetchHttpStatCodeLine(httpResp);
			apiResp += this.fetchHttpHeaderLines(httpHead);
			apiResp += "\n\n" + httpResp.body();

		}
		mav.addObject("api_resp", apiResp);

		return mav;
	}
	// }}}

	// {{{ private String makeSendUrl(String apiPath)
	private String makeSendUrl(String apiPath)
	{
		String apiUrl = this.apiHost + apiPath;
		if (apiPath.startsWith("/userinfo") == true) {
			OidcConfigResponse respConfig = this.oidcConfigRepository.discovery();
			apiUrl = respConfig.getUserinfo_endpoint();
		}
		return apiUrl;
	}
	// }}}

	// {{{ private HttpResponse<String> sendApiServer(String apiUrl, String access_token)
	private HttpResponse<String> sendApiServer(String apiUrl, String access_token)
	{
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl))
			.GET();
		if ((access_token != null) && (access_token.isEmpty() == false)) {
			builder.setHeader("Authorization", "Bearer " + access_token);
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

}
