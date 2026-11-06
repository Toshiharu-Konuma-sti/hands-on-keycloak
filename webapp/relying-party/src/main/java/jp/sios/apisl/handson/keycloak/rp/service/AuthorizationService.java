package jp.sios.apisl.handson.keycloak.rp.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import jp.sios.apisl.handson.keycloak.rp.config.RpProperties;
import jp.sios.apisl.handson.keycloak.rp.model.ClientInfo;
import jp.sios.apisl.handson.keycloak.rp.model.LoginError;
import jp.sios.apisl.handson.keycloak.rp.model.Step;
import jp.sios.apisl.handson.keycloak.rp.service.HttpSender.Response;
import jp.sios.apisl.handson.keycloak.rp.support.SessionStore;
import jp.sios.apisl.handson.keycloak.rp.support.Tokens;
import tools.jackson.databind.json.JsonMapper;

/**
 * Authorization Code + PKCE の、1(認可リクエスト)から4(IDトークンの検証)までと、ログアウト。
 * 各ステップの通信内容は、画面の「通信のトレース」に出すため、SessionStoreのstepsに記録する。
 */
@Service
public class AuthorizationService {

	private static final String AUTH_PATH = "/protocol/openid-connect/auth";
	private static final String TOKEN_PATH = "/protocol/openid-connect/token";
	private static final String LOGOUT_PATH = "/protocol/openid-connect/logout";

	private final RpProperties props;
	private final Catalog catalog;
	private final HttpSender http;
	private final JsonMapper json;

	public AuthorizationService(RpProperties props, Catalog catalog, HttpSender http, JsonMapper json) {
		this.props = props;
		this.catalog = catalog;
		this.http = http;
		this.json = json;
	}

	// ---- 1. 認可リクエスト ------------------------------------------------------------------

	// 認可リクエストを作り、ブラウザを移動させるKeycloakのURLを返す。アプリが未選択や不明のときは、何もせず空を返す
	public Optional<URI> startLogin(String client, List<String> optionalScopes, SessionStore store) {
		Optional<ClientInfo> selected = catalog.findClient(client);
		if (selected.isEmpty()) {
			return Optional.empty();
		}
		String clientId = selected.get().id();
		// 画面で選んだoptional scopeを、アプリを問わずそのまま要求する。付与するか拒否するかはKeycloakが決める
		List<String> requested = catalog.chooseOptionalScopes(optionalScopes);
		String scope = "openid" + requested.stream().map(s -> " " + s).reduce("", String::concat);

		String state = Tokens.random(16);               // CSRF対策: 戻ってきたときに一致を確認する
		String nonce = Tokens.random(16);               // リプレイ対策: IDトークンに入って戻ってくる
		String verifier = Tokens.random(32);            // PKCE: 秘密の値。トークン交換のときに送る
		String challenge = Tokens.s256(verifier);       // PKCE: そのハッシュ値。認可リクエストで先に送る

		store.startLogin(clientId, requested, state, nonce, verifier);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("response_type", "code");
		params.put("client_id", clientId);
		params.put("redirect_uri", props.redirectUri());
		params.put("scope", scope);
		params.put("state", state);
		params.put("nonce", nonce);
		params.put("code_challenge", challenge);
		params.put("code_challenge_method", "S256");

		String authEndpoint = props.keycloak().browserUrl() + AUTH_PATH;
		UriComponentsBuilder url = UriComponentsBuilder.fromUriString(authEndpoint);
		params.forEach(url::queryParam);

		store.steps().add(new Step(
			"1. 認可リクエスト(ブラウザ → Keycloak)",
			"RPはブラウザをKeycloakのログイン画面へ移動させる。どのアプリが(client_id)、何を許してほしいか(scope)を伝える。",
			"GET " + authEndpoint + "\n" + lines(params),
			"(この応答はブラウザとKeycloakの間でやり取りされるため、RPには見えない。結果は、ブラウザがRPに戻ってきたときに分かる)"));
		return Optional.of(url.build().encode().toUri());
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
		String response = "HTTP 302 Found\nLocation: " + props.redirectUri() + "\n" + lines(returned);
		steps.set(0, new Step(first.title(), first.note(), first.request(), screen, response));
	}

	// ---- 2. Keycloakから戻る → 3. トークン交換 → 4. IDトークンの検証 -----------------------------

	// 結果は、成功ならstoreのトークン、失敗ならstoreのloginErrorかトレースに残る
	public void handleCallback(String code, String state, String error, String errorDescription, SessionStore store) {
		List<Step> steps = store.steps();
		String redirectUri = props.redirectUri();

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
			store.saveLoginError(new LoginError(error, errorDescription, hint));
			return;
		}
		if (code == null || !Objects.equals(state, store.state())) {
			steps.add(new Step("state の不一致", "戻ってきたstateが、最初に送った値と違う。処理を中止する。", "", ""));
			return;
		}

		String clientId = store.clientId();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("redirect_uri", redirectUri);
		form.add("client_id", clientId);
		form.add("client_secret", props.clientSecret());
		form.add("code_verifier", store.verifier());

		String tokenEndpoint = props.keycloak().serverUrl() + TOKEN_PATH;
		Response res = http.postForm(tokenEndpoint, form);

		Map<String, String> shownForm = new LinkedHashMap<>();
		form.forEach((k, v) -> shownForm.put(k, switch (k) {
			case "client_secret" -> "******(表示用に隠している)";
			case "code" -> Tokens.abbreviate(v.get(0));
			default -> v.get(0);
		}));
		Map<String, Object> body = http.parseMap(res.body());
		steps.add(new Step(
			"3. codeをトークンに交換(RP → Keycloak)",
			"ブラウザを通らない直接の通信(バックチャネル)。client_secretとcode_verifier(PKCE)で、RP本人であることを示す。",
			"POST " + tokenEndpoint + "\n" + lines(shownForm),
			"HTTP " + res.status() + "\n" + http.pretty(abbreviateTokens(body))));
		if (res.status() != 200) {
			return;
		}

		String accessToken = (String) body.get("access_token");
		String idToken = (String) body.get("id_token");
		Map<String, Object> accessClaims = Tokens.claimsOf(json, accessToken);
		Map<String, Object> idClaims = Tokens.claimsOf(json, idToken);

		List<String> checks = new ArrayList<>();
		boolean ok = checkIdToken(idClaims, clientId, store.nonce(), checks);
		steps.add(new Step(
			"4. IDトークンの検証(RP)",
			"IDトークンは「誰がログインしたか」を表す。発行者・宛先・nonce・期限を確認する。",
			String.join("\n", checks),
			ok ? "OK: ログインを受け付ける" : "NG: ログインを受け付けない"));
		if (ok) {
			store.saveTokens(accessToken, idToken, accessClaims, idClaims);
		}
	}

	private boolean checkIdToken(Map<String, Object> claims, String clientId, String nonce, List<String> out) {
		String browserUrl = props.keycloak().browserUrl();
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

	// ---- ログアウト ------------------------------------------------------------------------

	// RPのセッションだけでなく、Keycloakのセッションも終了するためのURL
	public URI logoutUrl(String idToken) {
		return UriComponentsBuilder.fromUriString(props.keycloak().browserUrl() + LOGOUT_PATH)
			.queryParam("id_token_hint", idToken)
			.queryParam("post_logout_redirect_uri", props.redirectUri().replace("/callback", "/"))
			.build().encode().toUri();
	}

	// ---- 補助 -----------------------------------------------------------------------------

	private static Map<String, Object> abbreviateTokens(Map<String, Object> body) {
		Map<String, Object> shown = new LinkedHashMap<>(body);
		for (String key : List.of("access_token", "refresh_token", "id_token")) {
			if (shown.get(key) instanceof String token) {
				shown.put(key, Tokens.abbreviate(token));
			}
		}
		return shown;
	}

	private static String lines(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		params.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
		return sb.toString().stripTrailing();
	}
}
