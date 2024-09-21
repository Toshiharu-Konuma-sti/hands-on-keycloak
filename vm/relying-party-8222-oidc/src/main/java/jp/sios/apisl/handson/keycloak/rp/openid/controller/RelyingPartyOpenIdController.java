package jp.sios.apisl.handson.keycloak.rp.openid.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import jp.sios.apisl.handson.keycloak.rp.openid.service.RelyingPartyOpenIdService;

@Controller
public class RelyingPartyOpenIdController {

	private final RelyingPartyOpenIdService service;

	// {{{ public RelyingPartyOpenIdController(RelyingPartyOpenIdService service)
	public RelyingPartyOpenIdController(RelyingPartyOpenIdService service)
	{
		this.service = service;
	}
	// }}}

	// {{{ public ModelAndView index(OAuth2AuthenticationToken token, ModelAndView model)
	@RequestMapping(value = {"/"})
	public ModelAndView index(OAuth2AuthenticationToken token, ModelAndView model)
	{
		String viewName = this.service.webapp(token, model);
		model.setViewName(viewName);
		return model;
	}
	// }}}

	// {{{ public String refresh(OAuth2AuthenticationToken token)
	@RequestMapping(value = {"/refresh"})
	public String refresh(OAuth2AuthenticationToken token)
	{
		String viewName = this.service.refresh(token);
		return viewName;
	}
	// }}}

	// {{{ public String logout(...)
	@RequestMapping(value = {"/mylogout"})
	public String logout(OAuth2AuthenticationToken token)
	{
		String viewName = this.service.logout(token);
		return viewName;
	}
	// }}}

	// {{{ public ModelAndView broker(...)
	@RequestMapping(path = "/broker", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView broker(
			OAuth2AuthenticationToken token,
			ModelAndView model,
			@RequestParam(name = "api_path", required = false) String api_path
		)
	{
		String viewName = this.service.broker(token, model, api_path);
		model.setViewName(viewName);
		return model;
	}
	// }}}

}
