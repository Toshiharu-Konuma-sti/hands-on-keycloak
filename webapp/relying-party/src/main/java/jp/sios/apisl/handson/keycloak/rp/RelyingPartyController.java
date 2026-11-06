package jp.sios.apisl.handson.keycloak.rp;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpSession;
import tools.jackson.databind.json.JsonMapper;

/**
 * Relying Party。Authorization Code + PKCE の流れを、順番どおりに手で実装している。
 *
 *   1. POST /login     : 認可リクエストを作り、ブラウザをKeycloakへ移動させる
 *   2. GET  /callback  : Keycloakから戻ってくる。codeをトークンに交換し(3)、IDトークンを検証する(4)
 *   5. POST /call      : アクセストークンを付けてResource Serverを呼ぶ
 *
 * /call と /clear は、画面のJavaScript(fetch)から呼ばれたときは、画面の一部(結果とトレース)だけを返す。
 * ログインやログアウトと違い、セッションが続いたまま何度も試す操作なので、ページ全体は読み直さない。
 *
 * 各ステップの通信内容は、画面の「通信のトレース」に表示する。
 */
@Controller
public class RelyingPartyController {

	private static final String AUTH_PATH = "/protocol/openid-connect/auth";
	private static final String TOKEN_PATH = "/protocol/openid-connect/token";
	private static final String LOGOUT_PATH = "/protocol/openid-connect/logout";

	// Keycloakに登録済みのクライアント(アプリ)。
	// どのスコープを許すかはクライアントごとにKeycloak側で決まっており、RPはここでは判断しない。
	record ClientInfo(String id, String label) {
	}

	private static final List<ClientInfo> CLIENTS = List.of(
		new ClientInfo("app-expense-claim", "経費申請アプリ"),
		new ClientInfo("app-expense-approval", "経費承認アプリ"));

	// 画面で選べるoptional scope。どのアプリでも自由に要求できる。
	// アプリに割り当てていないスコープを要求すると、Keycloakがエラー(invalid_scope)で拒否する。
	// (expenses:read はdefault scopeなので、要求しなくても常に付く)
	private static final List<String> OPTIONAL_SCOPES = List.of("expenses:write", "expenses:approve");

	// ログインに失敗してKeycloakから戻ってきたときの理由
	public record LoginError(String error, String description, String hint) {
	}

	record Api(String label, String method, String path, String rule, String body) {
		public boolean needsId() {
			return path.contains("{id}");
		}
	}

	private static final List<Api> APIS = List.of(
		new Api("whoami", "GET", "/apis/whoami", "認証のみ", null),
		new Api("経費の一覧(自分の分)", "GET", "/apis/expenses", "スコープ expenses:read", null),
		new Api("経費を申請する", "POST", "/apis/expenses", "スコープ expenses:write",
			"{\"title\":\"出張宿泊費\",\"amount\":9800}"),
		new Api("承認待ちの一覧", "GET", "/apis/expenses/pending", "スコープ expenses:approve かつ ロール my-manager-role(課長以上)", null),
		new Api("経費を承認する", "POST", "/apis/expenses/{id}/approve", "スコープ expenses:approve かつ ロール my-admin-role(部長)", null));

	private record Http(int status, String body) {
	}

	// 画面の「結果」と「履歴」に表示する、API呼び出し1回分の記録
	public record CallResult(int seq, String method, String path, int status, String message,
			String required, List<String> grantedScopes, List<String> grantedRoles, String body) {
		public String statusClass() {
			return status >= 200 && status < 300 ? "ok" : "ng";
		}
	}

	// fetchで呼ばれたときに付くヘッダ。付いていれば画面の一部だけを返す
	private static final String FETCH_HEADER = "X-Requested-With";

	private final String browserUrl;
	private final String serverUrl;
	private final String resourceServerUrl;
	private final String redirectUri;
	private final String clientSecret;
	private final JsonMapper json;
	private final RestClient http = RestClient.create();

	public RelyingPartyController(
			@Value("${handson.keycloak.browser-url}") String browserUrl,
			@Value("${handson.keycloak.server-url}") String serverUrl,
			@Value("${handson.resource-server-url}") String resourceServerUrl,
			@Value("${handson.redirect-uri}") String redirectUri,
			@Value("${handson.client-secret}") String clientSecret,
			JsonMapper json) {
		this.browserUrl = browserUrl;
		this.serverUrl = serverUrl;
		this.resourceServerUrl = resourceServerUrl;
		this.redirectUri = redirectUri;
		this.clientSecret = clientSecret;
		this.json = json;
	}

	@GetMapping("/")
	String index(HttpSession session, Model model) {
		populate(session, model);
		return "index";
	}

	private void populate(HttpSession session, Model model) {
		Map<String, Object> access = attr(session, "accessClaims");
		model.addAttribute("loggedIn", access != null);
		model.addAttribute("clients", CLIENTS);
		// ログイン時に選んだ内容をフォームに復元する。未ログインやログアウト後は初期状態(先頭のアプリ、チェックなし)
		Object selected = session.getAttribute("clientId");
		model.addAttribute("selectedClient", selected != null ? selected : CLIENTS.get(0).id());
		model.addAttribute("optionalScopes", OPTIONAL_SCOPES);
		model.addAttribute("selectedScopes", session.getAttribute("requestedScopes") != null
			? session.getAttribute("requestedScopes") : List.of());
		model.addAttribute("loginError", session.getAttribute("loginError"));
		model.addAttribute("apis", APIS);
		model.addAttribute("steps", steps(session));
		List<CallResult> calls = calls(session);
		model.addAttribute("latest", calls.isEmpty() ? null : calls.get(calls.size() - 1));
		model.addAttribute("history", calls.reversed());
		if (access != null) {
			model.addAttribute("clientId", session.getAttribute("clientId"));
			model.addAttribute("username", access.get("preferred_username"));
			model.addAttribute("scopes", List.of(String.valueOf(access.getOrDefault("scope", "")).split(" ")));
			model.addAttribute("audience", Tokens.audienceOf(access));
			model.addAttribute("roles", realmRoles(access));
			model.addAttribute("accessJson", pretty(access));
			model.addAttribute("idJson", pretty(session.getAttribute("idClaims")));
		}
	}

	// ---- 1. 認可リクエスト ------------------------------------------------------------------

	@PostMapping("/login")
	String login(@RequestParam String client,
			@RequestParam(name = "scope", required = false) List<String> optionalScopes,
			HttpSession session) {
		ClientInfo selected = CLIENTS.stream().filter(c -> c.id().equals(client)).findFirst().orElse(CLIENTS.get(0));
		String clientId = selected.id();
		// 画面で選んだoptional scopeを、アプリを問わずそのまま要求する。付与するか拒否するかはKeycloakが決める
		List<String> requested = OPTIONAL_SCOPES.stream()
			.filter(s -> optionalScopes != null && optionalScopes.contains(s)).toList();
		String scope = "openid" + requested.stream().map(s -> " " + s).reduce("", String::concat);

		String state = Tokens.random(16);               // CSRF対策: 戻ってきたときに一致を確認する
		String nonce = Tokens.random(16);               // リプレイ対策: IDトークンに入って戻ってくる
		String verifier = Tokens.random(32);            // PKCE: 秘密の値。トークン交換のときに送る
		String challenge = Tokens.s256(verifier);       // PKCE: そのハッシュ値。認可リクエストで先に送る

		// 前回のログイン結果を捨てて、最初からやり直す
		for (String key : List.of("accessToken", "idToken", "accessClaims", "idClaims", "steps", "calls")) {
			session.removeAttribute(key);
		}
		session.removeAttribute("loginError");
		session.setAttribute("clientId", clientId);
		session.setAttribute("requestedScopes", requested);
		session.setAttribute("state", state);
		session.setAttribute("nonce", nonce);
		session.setAttribute("verifier", verifier);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("response_type", "code");
		params.put("client_id", clientId);
		params.put("redirect_uri", redirectUri);
		params.put("scope", scope);
		params.put("state", state);
		params.put("nonce", nonce);
		params.put("code_challenge", challenge);
		params.put("code_challenge_method", "S256");

		UriComponentsBuilder url = UriComponentsBuilder.fromUriString(browserUrl + AUTH_PATH);
		params.forEach(url::queryParam);
		URI authUrl = url.build().encode().toUri();

		steps(session).add(new Step(
			"1. 認可リクエスト(ブラウザ → Keycloak)",
			"RPはブラウザをKeycloakのログイン画面へ移動させる。どのアプリが(client_id)、何を許してほしいか(scope)を伝える。",
			"GET " + browserUrl + AUTH_PATH + "\n" + lines(params),
			"(この応答はブラウザとKeycloakの間でやり取りされるため、RPには見えない。結果は、ブラウザがRPに戻ってきたときに分かる)"));
		return "redirect:" + authUrl;
	}

	// 1の応答は、ブラウザとKeycloakの間のやり取りでRPには見えない。
	// ブラウザがRPに戻ってきたときの内容から、何が起きたかを補って表示する。
	private void completeStep1(List<Step> steps, boolean failed, Map<String, String> returned) {
		if (steps.isEmpty() || !steps.get(0).title().startsWith("1.")) {
			return;
		}
		Step first = steps.get(0);
		String screen = failed
			? "ログイン画面は表示されなかった(Keycloakがリクエストをエラーと判断した)"
			: "Keycloakのログイン画面と同意画面が表示され、ユーザーが操作した";
		String response = "HTTP 302 Found\nLocation: " + redirectUri + "\n" + lines(returned);
		steps.set(0, new Step(first.title(), first.note(), first.request(), screen, response));
	}

	// ---- 2. Keycloakから戻る → 3. トークン交換 → 4. IDトークンの検証 -----------------------------

	@GetMapping("/callback")
	String callback(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error,
			@RequestParam(name = "error_description", required = false) String errorDescription,
			HttpSession session) {
		List<Step> steps = steps(session);

		Map<String, String> returned = new LinkedHashMap<>();
		if (code != null) returned.put("code", Tokens.abbreviate(code));
		if (state != null) returned.put("state", state);
		if (error != null) returned.put("error", error);
		if (errorDescription != null) returned.put("error_description", errorDescription);
		completeStep1(steps, error != null, returned);
		steps.add(new Step(
			"2. Keycloakから戻る(ブラウザ → RP)",
			error != null
				? "Keycloakが返したエラー(上の1の応答の302)に従って、ブラウザがRPに戻ってきた。codeは付かず、エラーの内容だけが付いている。"
				: "ログインと同意が済むと、認可コード(code)を付けてRPに戻ってくる。codeはまだ「引換券」で、APIは呼べない。",
			"GET " + redirectUri + "\n" + lines(returned),
			error != null ? "(エラーのため、トークンには交換しない)" : "(RPはこのcodeをトークンに交換する)"));

		if (error != null) {
			// 失敗の理由を画面の上部に表示する
			String hint = "invalid_scope".equals(error)
				? "選んだアプリに割り当てられていないスコープを要求したため、Keycloakがログインを拒否した。"
					+ "アプリを変えるか、要求するスコープを見直す。"
				: null;
			session.setAttribute("loginError", new LoginError(error, errorDescription, hint));
			return "redirect:/";
		}
		if (code == null || !Objects.equals(state, session.getAttribute("state"))) {
			steps.add(new Step("state の不一致", "戻ってきたstateが、最初に送った値と違う。処理を中止する。", "", ""));
			return "redirect:/";
		}

		String clientId = (String) session.getAttribute("clientId");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("redirect_uri", redirectUri);
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("code_verifier", (String) session.getAttribute("verifier"));

		Http res = send(http.post().uri(serverUrl + TOKEN_PATH)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form));

		Map<String, String> shownForm = new LinkedHashMap<>();
		form.forEach((k, v) -> shownForm.put(k, switch (k) {
			case "client_secret" -> "******(表示用に隠している)";
			case "code" -> Tokens.abbreviate(v.get(0));
			default -> v.get(0);
		}));
		Map<String, Object> body = parseMap(res.body());
		steps.add(new Step(
			"3. codeをトークンに交換(RP → Keycloak)",
			"ブラウザを通らない直接の通信(バックチャネル)。client_secretとcode_verifier(PKCE)で、RP本人であることを示す。",
			"POST " + serverUrl + TOKEN_PATH + "\n" + lines(shownForm),
			"HTTP " + res.status() + "\n" + pretty(abbreviateTokens(body))));
		if (res.status() != 200) {
			return "redirect:/";
		}

		String accessToken = (String) body.get("access_token");
		String idToken = (String) body.get("id_token");
		Map<String, Object> accessClaims = Tokens.claimsOf(json, accessToken);
		Map<String, Object> idClaims = Tokens.claimsOf(json, idToken);

		List<String> checks = new ArrayList<>();
		boolean ok = checkIdToken(idClaims, clientId, (String) session.getAttribute("nonce"), checks);
		steps.add(new Step(
			"4. IDトークンの検証(RP)",
			"IDトークンは「誰がログインしたか」を表す。発行者・宛先・nonce・期限を確認する。",
			String.join("\n", checks),
			ok ? "OK: ログインを受け付ける" : "NG: ログインを受け付けない"));
		if (!ok) {
			return "redirect:/";
		}

		session.setAttribute("accessToken", accessToken);
		session.setAttribute("idToken", idToken);
		session.setAttribute("accessClaims", accessClaims);
		session.setAttribute("idClaims", idClaims);
		return "redirect:/";
	}

	// ---- 5. APIの呼び出し -----------------------------------------------------------------

	@PostMapping("/call")
	String call(@RequestParam String method, @RequestParam String path,
			@RequestParam(required = false) Integer id,
			@RequestHeader(name = FETCH_HEADER, required = false) String fetch,
			HttpSession session, Model model) {
		String token = (String) session.getAttribute("accessToken");
		Api api = APIS.stream()
			.filter(a -> a.method().equals(method) && a.path().equals(path)).findFirst().orElse(null);
		if (token == null || api == null) {
			return "redirect:/";
		}
		String actualPath = api.needsId() ? path.replace("{id}", String.valueOf(id == null ? 1 : id)) : path;
		String url = resourceServerUrl + actualPath;

		RestClient.RequestHeadersSpec<?> request;
		if (api.method().equals("GET")) {
			request = http.get().uri(url);
		} else if (api.body() == null) {
			request = http.post().uri(url);
		} else {
			request = http.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(api.body());
		}
		Http res = send(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

		String shownRequest = api.method() + " " + url + "\nAuthorization: Bearer " + Tokens.abbreviate(token)
			+ (api.body() == null ? "" : "\nContent-Type: application/json\n\n" + api.body());
		steps(session).add(new Step(
			"5. APIを呼ぶ: " + api.method() + " " + actualPath + "  →  " + res.status(),
			"アクセストークンをAuthorizationヘッダに付けて、Resource Serverを呼ぶ。許可するかどうかは、RSがトークンの中身で判断する。",
			shownRequest,
			"HTTP " + res.status() + "\n" + pretty(parseAny(res.body()))));
		calls(session).add(toCallResult(calls(session).size() + 1, api.method(), actualPath, res));
		return fetch != null ? updates(session, model) : "redirect:/";
	}

	// 画面の一部(結果とトレース)だけを返す
	private String updates(HttpSession session, Model model) {
		populate(session, model);
		return "fragments :: updates";
	}

	// 403のときはRSが返すrequired/grantedを取り出して、画面で見比べられるようにする
	private CallResult toCallResult(int seq, String method, String path, Http res) {
		Object parsed = parseAny(res.body());
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
		return new CallResult(seq, method, path, res.status(), message, required, scopes, roles, pretty(parsed));
	}

	private static List<String> strings(Object value) {
		return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
	}

	// ---- ログアウト・トレースの消去 -----------------------------------------------------------

	@PostMapping("/logout")
	String logout(HttpSession session) {
		String idToken = (String) session.getAttribute("idToken");
		session.invalidate();
		if (idToken == null) {
			return "redirect:/";
		}
		// RPのセッションだけでなく、Keycloakのセッションも終了する(別のユーザーでログインし直せる)
		URI url = UriComponentsBuilder.fromUriString(browserUrl + LOGOUT_PATH)
			.queryParam("id_token_hint", idToken)
			.queryParam("post_logout_redirect_uri", redirectUri.replace("/callback", "/"))
			.build().encode().toUri();
		return "redirect:" + url;
	}

	@PostMapping("/clear")
	String clear(@RequestHeader(name = FETCH_HEADER, required = false) String fetch,
			HttpSession session, Model model) {
		steps(session).clear();
		calls(session).clear();
		return fetch != null ? updates(session, model) : "redirect:/";
	}

	// ---- 補助 -----------------------------------------------------------------------------

	private boolean checkIdToken(Map<String, Object> claims, String clientId, String nonce, List<String> out) {
		boolean issOk = browserUrl.equals(claims.get("iss"));
		boolean audOk = Tokens.audienceOf(claims).contains(clientId);
		boolean nonceOk = Objects.equals(nonce, claims.get("nonce"));
		boolean expOk = ((Number) claims.get("exp")).longValue() > System.currentTimeMillis() / 1000;
		out.add((issOk ? "OK" : "NG") + "  iss   = " + claims.get("iss") + "   (期待: " + browserUrl + ")");
		out.add((audOk ? "OK" : "NG") + "  aud   = " + Tokens.audienceOf(claims) + "   (期待: " + clientId + " を含む)");
		out.add((nonceOk ? "OK" : "NG") + "  nonce = " + claims.get("nonce") + "   (認可リクエストで送った値と一致)");
		out.add((expOk ? "OK" : "NG") + "  exp   = " + claims.get("exp") + "   (現在時刻より後)");
		if (!issOk) {
			out.add("ヒント: issがbrowser-urlと違う場合は、KeycloakのKC_HOSTNAMEの設定を確認する");
		}
		return issOk && audOk && nonceOk && expOk;
	}

	@SuppressWarnings("unchecked")
	private static List<String> realmRoles(Map<String, Object> claims) {
		Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
		return realmAccess == null ? List.of() : (List<String>) realmAccess.get("roles");
	}

	private Map<String, Object> abbreviateTokens(Map<String, Object> body) {
		Map<String, Object> shown = new LinkedHashMap<>(body);
		for (String key : List.of("access_token", "refresh_token", "id_token")) {
			if (shown.get(key) instanceof String token) {
				shown.put(key, Tokens.abbreviate(token));
			}
		}
		return shown;
	}

	private Http send(RestClient.RequestHeadersSpec<?> request) {
		try {
			return request.exchange((req, res) -> new Http(res.getStatusCode().value(), res.bodyTo(String.class)));
		} catch (Exception e) {
			return new Http(0, e.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseMap(String text) {
		Object value = parseAny(text);
		return value instanceof Map ? (Map<String, Object>) value : Map.of();
	}

	private Object parseAny(String text) {
		try {
			return json.readValue(text, Object.class);
		} catch (Exception e) {
			return text;
		}
	}

	private String pretty(Object value) {
		if (value instanceof String s) {
			return s;
		}
		return json.writerWithDefaultPrettyPrinter().writeValueAsString(value);
	}

	private static String lines(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		params.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
		return sb.toString().stripTrailing();
	}

	@SuppressWarnings("unchecked")
	private static <T> T attr(HttpSession session, String name) {
		return (T) session.getAttribute(name);
	}

	@SuppressWarnings("unchecked")
	private static List<Step> steps(HttpSession session) {
		List<Step> steps = (List<Step>) session.getAttribute("steps");
		if (steps == null) {
			steps = new ArrayList<>();
			session.setAttribute("steps", steps);
		}
		return steps;
	}

	@SuppressWarnings("unchecked")
	private static List<CallResult> calls(HttpSession session) {
		List<CallResult> calls = (List<CallResult>) session.getAttribute("calls");
		if (calls == null) {
			calls = new ArrayList<>();
			session.setAttribute("calls", calls);
		}
		return calls;
	}
}
