package jp.sios.apisl.handson.keycloak.rp.auth.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import jp.sios.apisl.handson.keycloak.rp.auth.entity.OidcConfigResponse;

/**
 * OpenID Connect Configuration Repository Implement Class
 */
@Repository
public class OidcConfigRepositoryImpl implements OidcConfigRepository
{
	@Value("${handson.relying-party.openid.config-uri}")
	private String confUrl;

	private final RestTemplate restTemplate;

	// {{{ public OidcConfigRepositoryImpl(RestTemplateBuilder restTemplateBuilder)
	public OidcConfigRepositoryImpl(RestTemplateBuilder restTemplateBuilder)
	{
		this.restTemplate = restTemplateBuilder.build();
	}
	// }}}

	// {{{ public OidcConfigResponse discovery()
	@Override
	public OidcConfigResponse discovery()
	{
		OidcConfigResponse resp =
			this.restTemplate.getForObject(this.confUrl, OidcConfigResponse.class);
		return resp;
	}
	// }}}

}
