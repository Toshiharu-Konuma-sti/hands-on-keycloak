package jp.sios.apisl.handson.keycloak.rlypty.repository;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import jp.sios.apisl.handson.keycloak.rlypty.entity.OidcTokenResponse;

/**
 * OpenID Connect Token Repository Implement Class
 */
@Repository
public class OidcTokenRepositoryImpl implements OidcTokenRepository
{
	private final RestTemplate restTemplate;

	// {{{ public OidcTokenRepositoryImpl(RestTemplate restTemplate)
	public OidcTokenRepositoryImpl(RestTemplateBuilder restTemplateBuilder)
	{
		this.restTemplate = restTemplateBuilder.build();
	}
	// }}}

	// {{{ public OidcTokenResponse generate(String url, String clientId, String clientSecret, String code, String redirect)
	@Override
	public OidcTokenResponse generate(String url, String clientId, String clientSecret, String code, String redirect)
	{
		MultiValueMap<String, String> param = new LinkedMultiValueMap<String, String>();
		param.add("grant_type", "authorization_code");
		param.add("client_id", clientId);
		param.add("client_secret", clientSecret);
		param.add("code", code);
		param.add("redirect_uri", redirect);
		OidcTokenResponse resp = this.restTemplate.postForObject(url, param, OidcTokenResponse.class);

		return resp;
	}
	// }}}

	// {{{ public OidcTokenResponse refresh(String url, String clientId, String clientSecret, String refreshToken)
	@Override
	public OidcTokenResponse refresh(String url, String clientId, String clientSecret, String refreshToken)
	{
		MultiValueMap<String, String> param = new LinkedMultiValueMap<String, String>();
		param.add("grant_type", "refresh_token");
		param.add("client_id", clientId);
		param.add("client_secret", clientSecret);
		param.add("refresh_token", refreshToken);
		OidcTokenResponse resp = this.restTemplate.postForObject(url, param, OidcTokenResponse.class);

		return resp;
	}
	// }}}

}
