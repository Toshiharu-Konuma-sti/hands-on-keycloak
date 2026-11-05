package jp.sios.apisl.handson.keycloak.ressrv.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// アクセストークンが無い/不正なときの401を、理由つきのJSONで返す
@Component
public class ApiErrorWriter {

	private final JsonMapper json;

	public ApiErrorWriter(JsonMapper json) {
		this.json = json;
	}

	public void unauthorized(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException e) throws IOException {
		// WWW-Authenticateヘッダと401は標準の処理に任せる
		new BearerTokenAuthenticationEntryPoint().commence(request, response, e);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", 401);
		body.put("error", "unauthorized");
		body.put("message", "アクセストークンが無い、または無効です");
		body.put("detail", e.getMessage());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json.writeValueAsString(body));
	}
}
