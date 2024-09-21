package jp.sios.apisl.handson.keycloak.rp.auth.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OidcConfigResponse
{
	private String issuer;
	private String authorization_endpoint;
	private String token_endpoint;
	private String userinfo_endpoint;
	private String end_session_endpoint;
	private String jwks_uri;

	// {{{ public OidcConfigResponse()
	public OidcConfigResponse()
	{
	}
	// }}}

	// {{{ public String toString()
	@Override
	public String toString() {
		return "issuer=[" + this.issuer + "]\n"
			+ "authorization_endpoint=[" + this.authorization_endpoint + "]\n"
			+ "token_endpoint=[" + this.token_endpoint + "]\n"
			+ "userinfo_endpoint=[" + this.userinfo_endpoint + "]\n"
			+ "jwks_uri=[" + this.jwks_uri + "]";
	}
	// }}}

}
