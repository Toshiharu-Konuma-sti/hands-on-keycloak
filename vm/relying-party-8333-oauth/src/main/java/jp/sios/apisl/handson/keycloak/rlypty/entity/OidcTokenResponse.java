package jp.sios.apisl.handson.keycloak.rlypty.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OidcTokenResponse
{
	private String access_token;
	private String token_type;
	private String refresh_token;
	private String expires_in;
	private String id_token;

	// {{{ public OidcTokenResponse()
	public OidcTokenResponse()
	{
	}
	// }}}

	// {{{ public OidcTokenResponse(String access_token, String token_type, String refresh_token, String expires_in, String id_token)
	public OidcTokenResponse(String access_token, String token_type, String refresh_token, String expires_in, String id_token)
	{
		this.access_token = access_token;
		this.token_type = token_type;
		this.refresh_token = refresh_token;
		this.expires_in = expires_in;
		this.id_token = id_token;
	}
	// }}}

	// {{{ public String toString()
	@Override
	public String toString() {
		return "access_token=[" + this.access_token + "]\n"
			+ "token_type=[" + this.token_type + "]\n"
			+ "refresh_token=[" + this.refresh_token + "]\n"
			+ "expires_in=[" + this.expires_in + "]\n"
			+ "id_token=[" + this.id_token + "]";
	}
	// }}}

}
