package jp.sios.apisl.handson.keycloak.rp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.sios.apisl.handson.keycloak.rp.model.CallResult;
import jp.sios.apisl.handson.keycloak.rp.service.ApiCallService;
import jp.sios.apisl.handson.keycloak.rp.service.AuthorizationService;
import jp.sios.apisl.handson.keycloak.rp.service.Catalog;
import jp.sios.apisl.handson.keycloak.rp.service.HttpSender;
import jp.sios.apisl.handson.keycloak.rp.support.SessionStore;
import jp.sios.apisl.handson.keycloak.rp.support.Tokens;

/**
 * Relying Party。Authorization Code + PKCE の流れを、順番どおりに手で実装している。
 *
 *   1. POST /login     : 認可リクエストを作り、ブラウザをKeycloakへ移動させる
 *   2. GET  /callback  : Keycloakから戻ってくる。codeをトークンに交換し(3)、IDトークンを検証する(4)
 *   5. POST /call      : アクセストークンを付けてResource Serverを呼ぶ
 *
 * このクラスはエンドポイントの入口だけを持つ。1〜4の詳細はAuthorizationService、5はApiCallServiceにある。
 *
 * /call と /clear は、画面のJavaScript(fetch)から呼ばれたときは、画面の一部(結果とトレース)だけを返す。
 * ログインやログアウトと違い、セッションが続いたまま何度も試す操作なので、ページ全体は読み直さない。
 *
 * 各ステップの通信内容は、画面の「通信のトレース」に表示する。
 */
@Controller
public class RelyingPartyController {

	// fetchで呼ばれたときに付くヘッダ。付いていれば画面の一部だけを返す
	private static final String FETCH_HEADER = "X-Requested-With";

	private final AuthorizationService authorization;
	private final ApiCallService apiCalls;
	private final Catalog catalog;
	private final HttpSender http;

	public RelyingPartyController(AuthorizationService authorization, ApiCallService apiCalls,
			Catalog catalog, HttpSender http) {
		this.authorization = authorization;
		this.apiCalls = apiCalls;
		this.catalog = catalog;
		this.http = http;
	}

	@GetMapping("/")
	String index(HttpSession session, Model model) {
		populate(new SessionStore(session), model);
		return "index";
	}

	// 1. 認可リクエスト
	@PostMapping("/login")
	String login(@RequestParam(required = false) String client,
			@RequestParam(name = "scope", required = false) List<String> optionalScopes,
			HttpSession session) {
		return authorization.startLogin(client, optionalScopes, new SessionStore(session))
			.map(url -> "redirect:" + url)
			.orElse("redirect:/");
	}

	// 2. Keycloakから戻る(3と4もここで行う)
	@GetMapping("/callback")
	String callback(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error,
			@RequestParam(name = "error_description", required = false) String errorDescription,
			HttpSession session) {
		authorization.handleCallback(code, state, error, errorDescription, new SessionStore(session));
		return "redirect:/";
	}

	// 5. APIの呼び出し
	@PostMapping("/call")
	String call(@RequestParam String method, @RequestParam String path,
			@RequestParam(required = false) Integer id,
			@RequestHeader(name = FETCH_HEADER, required = false) String fetch,
			HttpSession session, Model model) {
		SessionStore store = new SessionStore(session);
		if (!apiCalls.call(method, path, id, store)) {
			return "redirect:/";
		}
		return fetch != null ? updates(store, model) : "redirect:/";
	}

	@PostMapping("/logout")
	String logout(HttpSession session) {
		SessionStore store = new SessionStore(session);
		String idToken = store.idToken();
		store.invalidate();
		return idToken == null ? "redirect:/" : "redirect:" + authorization.logoutUrl(idToken);
	}

	@PostMapping("/clear")
	String clear(@RequestHeader(name = FETCH_HEADER, required = false) String fetch,
			HttpSession session, Model model) {
		SessionStore store = new SessionStore(session);
		store.clearTrace();
		return fetch != null ? updates(store, model) : "redirect:/";
	}

	// 画面の一部(結果とトレース)だけを返す
	private String updates(SessionStore store, Model model) {
		populate(store, model);
		return "fragments :: updates";
	}

	private void populate(SessionStore store, Model model) {
		Map<String, Object> access = store.accessClaims();
		model.addAttribute("loggedIn", access != null);
		model.addAttribute("clients", catalog.clients());
		// ログイン時に選んだ内容をフォームに復元する。未ログインやログアウト後は初期状態(アプリは未選択、チェックなし)
		model.addAttribute("selectedClient", store.clientId());
		// このRPがいま振る舞っているアプリ。ログインに成功したときだけ決まり、トークンの発行先(azp)から求める
		String azp = access == null ? null : Tokens.authorizedPartyOf(access);
		model.addAttribute("currentApp", azp == null ? null : catalog.findClient(azp).orElse(null));
		model.addAttribute("optionalScopes", catalog.optionalScopes());
		model.addAttribute("selectedScopes", store.requestedScopes());
		model.addAttribute("loginError", store.loginError());
		model.addAttribute("apis", catalog.apis());
		model.addAttribute("steps", store.steps());
		List<CallResult> calls = store.calls();
		model.addAttribute("latest", calls.isEmpty() ? null : calls.get(calls.size() - 1));
		model.addAttribute("history", calls.reversed());
		if (access != null) {
			model.addAttribute("azp", azp);
			model.addAttribute("username", access.get("preferred_username"));
			model.addAttribute("email", access.get("email"));
			model.addAttribute("scopes", List.of(String.valueOf(access.getOrDefault("scope", "")).split(" ")));
			model.addAttribute("audience", Tokens.audienceOf(access));
			model.addAttribute("roles", realmRoles(access));
			model.addAttribute("accessJson", http.pretty(access));
			model.addAttribute("idJson", http.pretty(store.idClaims()));
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> realmRoles(Map<String, Object> claims) {
		Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
		return realmAccess == null ? List.of() : (List<String>) realmAccess.get("roles");
	}
}
