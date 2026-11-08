package jp.sios.apisl.handson.keycloak.rp.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jp.sios.apisl.handson.keycloak.rp.config.RpProperties;
import jp.sios.apisl.handson.keycloak.rp.model.Api;
import jp.sios.apisl.handson.keycloak.rp.model.CallResult;
import jp.sios.apisl.handson.keycloak.rp.model.Step;
import jp.sios.apisl.handson.keycloak.rp.service.HttpSender.Response;
import jp.sios.apisl.handson.keycloak.rp.support.SessionStore;
import jp.sios.apisl.handson.keycloak.rp.support.Tokens;

// 5. アクセストークンを付けてResource Serverを呼び、結果をトレースと履歴に記録する
@Service
public class ApiCallService {

	private final RpProperties props;
	private final Catalog catalog;
	private final HttpSender http;

	public ApiCallService(RpProperties props, Catalog catalog, HttpSender http) {
		this.props = props;
		this.catalog = catalog;
		this.http = http;
	}

	// ログインしていない、または一覧にないAPIのときは、何もせず false を返す
	public boolean call(String method, String path, Integer id, SessionStore store) {
		String token = store.accessToken();
		Optional<Api> found = catalog.findApi(method, path);
		if (token == null || found.isEmpty()) {
			return false;
		}
		Api api = found.get();
		String actualPath = api.needsId() ? path.replace("{id}", String.valueOf(id == null ? 1 : id)) : path;
		String url = props.resourceServerUrl() + actualPath;

		Response res = http.callApi(api.method(), url, token, api.body());

		String shownRequest = api.method() + " " + url + "\nAuthorization: Bearer " + Tokens.abbreviate(token)
			+ (api.body() == null ? "" : "\nContent-Type: application/json\n\n" + api.body());
		store.steps().add(new Step(
			"5. APIを呼ぶ: " + api.method() + " " + actualPath + "  →  " + res.status(),
			"アクセストークンをAuthorizationヘッダに付けて、Resource Serverを呼ぶ。許可するかどうかは、RSがトークンの中身で判断する。",
			shownRequest,
			"HTTP " + res.status() + "\n" + http.pretty(http.parseAny(res.body()))));
		store.calls().add(toCallResult(store.calls().size() + 1, api.method(), actualPath, shownRequest, res));
		return true;
	}

	// 403のときはRSが返すrequired/grantedを取り出して、画面で見比べられるようにする
	private CallResult toCallResult(int seq, String method, String path, String request, Response res) {
		Object parsed = http.parseAny(res.body());
		String message = null;
		String required = null;
		List<String> scopes = List.of();
		List<String> roles = List.of();
		if (parsed instanceof Map<?, ?> map) {
			message = map.get("message") instanceof String s ? s : null;
			required = map.get("required") instanceof String s ? s : null;
			if (map.get("granted") instanceof Map<?, ?> granted) {
				scopes = strings(granted.get("scopes"));
				roles = strings(granted.get("roles"));
			}
			if (res.status() == 401 && map.get("detail") instanceof String d) {
				message = message + "(" + d + ")";
			}
		}
		return new CallResult(seq, method, path, res.status(), message, required, scopes, roles, request,
			"HTTP " + res.status() + "\n" + http.pretty(parsed));
	}

	private static List<String> strings(Object value) {
		return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
	}
}
