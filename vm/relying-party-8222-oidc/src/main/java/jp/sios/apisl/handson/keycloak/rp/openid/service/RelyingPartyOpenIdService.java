package jp.sios.apisl.handson.keycloak.rp.openid.service;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

/**
 * Relying Party OpenID Service Interface
 */
public interface RelyingPartyOpenIdService {

	public String webapp(OAuth2AuthenticationToken token, ModelAndView model);
	public String refresh(OAuth2AuthenticationToken token);
	public String logout(OAuth2AuthenticationToken token);
	public String broker(OAuth2AuthenticationToken token, ModelAndView model, String apiPath);

}
