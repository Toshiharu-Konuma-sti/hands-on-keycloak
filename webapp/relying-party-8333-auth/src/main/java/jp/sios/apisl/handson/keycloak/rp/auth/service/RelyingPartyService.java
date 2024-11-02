package jp.sios.apisl.handson.keycloak.rp.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * Relying Party Service Interface
 *
 */
public interface RelyingPartyService
{

	public ModelAndView webapp(
			HttpServletRequest request,
            ModelAndView mav,
			String code,
			String auth_code,
			String access_token, String refresh_token, String id_token,
			String token_type, String expires_in
		);

    public ModelAndView broker(ModelAndView mav, String apiPath, String access_token);

}
