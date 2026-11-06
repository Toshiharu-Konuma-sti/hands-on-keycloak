package jp.sios.apisl.handson.keycloak.rp.service;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

// KeycloakやResource Serverへの直接の通信(バックチャネル)と、その応答のJSONの扱い
@Component
public class HttpSender {

	// 通信の結果。接続に失敗したときは status が 0 で、body に例外の内容が入る
	public record Response(int status, String body) {
	}

	private final RestClient http = RestClient.create();
	private final JsonMapper json;

	public HttpSender(JsonMapper json) {
		this.json = json;
	}

	public Response postForm(String url, MultiValueMap<String, String> form) {
		return send(http.post().uri(url).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form));
	}

	// アクセストークンをAuthorizationヘッダに付けて、APIを呼ぶ
	public Response callApi(String method, String url, String accessToken, String body) {
		RestClient.RequestHeadersSpec<?> request;
		if (method.equals("GET")) {
			request = http.get().uri(url);
		} else if (body == null) {
			request = http.post().uri(url);
		} else {
			request = http.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(body);
		}
		return send(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private Response send(RestClient.RequestHeadersSpec<?> request) {
		try {
			return request.exchange((req, res) -> new Response(res.getStatusCode().value(), res.bodyTo(String.class)));
		} catch (Exception e) {
			return new Response(0, e.toString());
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> parseMap(String text) {
		Object value = parseAny(text);
		return value instanceof Map ? (Map<String, Object>) value : Map.of();
	}

	// JSONとして読めないときは、文字列のまま返す
	public Object parseAny(String text) {
		try {
			return json.readValue(text, Object.class);
		} catch (Exception e) {
			return text;
		}
	}

	public String pretty(Object value) {
		if (value instanceof String s) {
			return s;
		}
		return json.writerWithDefaultPrettyPrinter().writeValueAsString(value);
	}
}
