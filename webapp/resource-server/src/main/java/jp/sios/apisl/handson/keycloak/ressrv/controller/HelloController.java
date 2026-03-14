package jp.sios.apisl.handson.keycloak.ressrv.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/api/v1/hello")
    public Map<String, Object> hello(@AuthenticationPrincipal Jwt jwt) {
        // トークン内の情報をマップ形式で返却
        return Map.of(
            "message", "Hello, " + jwt.getClaimAsString("preferred_username") + "!",
            "subject", jwt.getSubject(),
            "claims", jwt.getClaims() // トークンの全中身を確認用
        );
    }
}