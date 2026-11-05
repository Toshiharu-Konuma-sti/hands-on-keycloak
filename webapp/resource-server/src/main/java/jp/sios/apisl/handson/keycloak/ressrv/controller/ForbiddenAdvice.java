package jp.sios.apisl.handson.keycloak.ressrv.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

// 認可に失敗したときの403を、「必要な条件」と「トークンが持っている権限」つきのJSONで返す
@RestControllerAdvice
public class ForbiddenAdvice {

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<Map<String, Object>> forbidden(HandlerMethod handler, Authentication auth) {
		List<String> scopes = new ArrayList<>();
		List<String> roles = new ArrayList<>();
		auth.getAuthorities().forEach(a -> {
			String name = a.getAuthority();
			if (name.startsWith("SCOPE_")) {
				scopes.add(name.substring("SCOPE_".length()));
			} else if (name.startsWith("ROLE_")) {
				roles.add(name.substring("ROLE_".length()));
			}
		});

		PreAuthorize rule = handler.getMethodAnnotation(PreAuthorize.class);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", 403);
		body.put("error", "forbidden");
		body.put("message", "このAPIに必要なスコープまたはロールが、アクセストークンにありません");
		body.put("required", rule == null ? null : rule.value());
		body.put("granted", Map.of("scopes", scopes, "roles", roles));
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
	}
}
