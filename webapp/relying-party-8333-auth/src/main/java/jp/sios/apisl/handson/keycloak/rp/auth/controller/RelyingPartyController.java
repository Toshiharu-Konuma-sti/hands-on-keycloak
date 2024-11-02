package jp.sios.apisl.handson.keycloak.rp.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import jp.sios.apisl.handson.keycloak.rp.auth.service.RelyingPartyService;

@Controller
public class RelyingPartyController {

	private final RelyingPartyService service;

	// {{{ public RelyingPartyController(RelyingPartyService service)
	public RelyingPartyController(RelyingPartyService service)
	{
		this.service = service;
	}
	// }}}

	// {{{ public String index()
	@RequestMapping(value = {"/", "/apps"})
	public String index()
	{
		return "redirect:/apps/";
	}
	// }}}

	// {{{ public String app()
	@RequestMapping("/apps/")
	public String app()
	{
//		return "apps/index";
		return "redirect:/apps/baseball";
	}
	// }}}

	// {{{ public ModelAndView webapp(...)
	@RequestMapping(value = {"/apps/baseball", "/apps/baseball-manager", "/apps/soccer", "/apps/soccer-manager"})
	public ModelAndView webapp(
		HttpServletRequest request,
		@RequestParam(name = "code", required = false) String code,
		@RequestParam(name = "auth_code", required = false) String auth_code,
		@RequestParam(name = "access_token", required = false) String access_token,
		@RequestParam(name = "refresh_token", required = false) String refresh_token,
		@RequestParam(name = "id_token", required = false) String id_token,
		@RequestParam(name = "token_type", required = false) String token_type,
		@RequestParam(name = "expires_in", required = false) String expires_in,
		ModelAndView mav)
	{
		mav = this.service.webapp(request, mav,
			code,
			auth_code,
			access_token, refresh_token, id_token,
			token_type, expires_in
		);
		mav.setViewName("apps/index");

		return mav;
	}
	// }}}

	// {{{ public ModelAndView broker(...)
	@RequestMapping(path = "/apps/broker", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView broker(
		@RequestParam(name = "api_path", required = false) String api_path,
		@RequestParam(name = "access_token", required = false) String access_token,
		ModelAndView mav)
	{
		mav = this.service.broker(mav, api_path, access_token);
		mav.setViewName("apps/broker");

		return mav;
	}
	// }}}

}
