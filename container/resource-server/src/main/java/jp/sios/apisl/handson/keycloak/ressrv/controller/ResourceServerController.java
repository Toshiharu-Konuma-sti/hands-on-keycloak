package jp.sios.apisl.handson.keycloak.ressrv.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ResourceServerController {

	@RequestMapping("/")
	public String index() {
		return "redirect:/apis";
	}

}
