package jp.sios.apisl.handson.keycloak.ressrv.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/v1/hello")
    public Map<String, Object> hello(Authentication authentication) {
        return Map.of(
            "message", "Hello, " + authentication.getName() + "!",
            "auth_type", authentication.getClass().getSimpleName(),
            "details", authentication.getPrincipal()
        );
    }
}