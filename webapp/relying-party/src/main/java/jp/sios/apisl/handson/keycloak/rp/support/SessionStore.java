package jp.sios.apisl.handson.keycloak.rp.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jp.sios.apisl.handson.keycloak.rp.model.CallResult;
import jp.sios.apisl.handson.keycloak.rp.model.LoginError;
import jp.sios.apisl.handson.keycloak.rp.model.Step;

// ブラウザごとのセッションに保存する内容の読み書き。セッションのキー名は、このクラスの中だけで使う
public class SessionStore {

	private static final String CLIENT_ID = "clientId";
	private static final String REQUESTED_SCOPES = "requestedScopes";
	private static final String STATE = "state";
	private static final String NONCE = "nonce";
	private static final String VERIFIER = "verifier";
	private static final String ACCESS_TOKEN = "accessToken";
	private static final String ID_TOKEN = "idToken";
	private static final String ACCESS_CLAIMS = "accessClaims";
	private static final String ID_CLAIMS = "idClaims";
	private static final String LOGIN_ERROR = "loginError";
	private static final String STEPS = "steps";
	private static final String CALLS = "calls";

	private final HttpSession session;

	public SessionStore(HttpSession session) {
		this.session = session;
	}

	// ---- ログインの途中 ----------------------------------------------------------------------

	// 前回のログイン結果を捨てて、新しいログインの途中の値(state, nonce, PKCEのverifier)を保存する
	public void startLogin(String clientId, List<String> requestedScopes, String state, String nonce, String verifier) {
		for (String key : List.of(ACCESS_TOKEN, ID_TOKEN, ACCESS_CLAIMS, ID_CLAIMS, STEPS, CALLS, LOGIN_ERROR)) {
			session.removeAttribute(key);
		}
		session.setAttribute(CLIENT_ID, clientId);
		session.setAttribute(REQUESTED_SCOPES, requestedScopes);
		session.setAttribute(STATE, state);
		session.setAttribute(NONCE, nonce);
		session.setAttribute(VERIFIER, verifier);
	}

	// ログインしていないときや、ログアウト後は null
	public String clientId() {
		return (String) session.getAttribute(CLIENT_ID);
	}

	public List<String> requestedScopes() {
		List<String> scopes = get(REQUESTED_SCOPES);
		return scopes == null ? List.of() : scopes;
	}

	public String state() {
		return (String) session.getAttribute(STATE);
	}

	public String nonce() {
		return (String) session.getAttribute(NONCE);
	}

	public String verifier() {
		return (String) session.getAttribute(VERIFIER);
	}

	// ---- ログインの結果 ----------------------------------------------------------------------

	public void saveTokens(String accessToken, String idToken,
			Map<String, Object> accessClaims, Map<String, Object> idClaims) {
		session.setAttribute(ACCESS_TOKEN, accessToken);
		session.setAttribute(ID_TOKEN, idToken);
		session.setAttribute(ACCESS_CLAIMS, accessClaims);
		session.setAttribute(ID_CLAIMS, idClaims);
	}

	public boolean loggedIn() {
		return accessClaims() != null;
	}

	public String accessToken() {
		return (String) session.getAttribute(ACCESS_TOKEN);
	}

	public String idToken() {
		return (String) session.getAttribute(ID_TOKEN);
	}

	public Map<String, Object> accessClaims() {
		return get(ACCESS_CLAIMS);
	}

	public Map<String, Object> idClaims() {
		return get(ID_CLAIMS);
	}

	public void saveLoginError(LoginError error) {
		session.setAttribute(LOGIN_ERROR, error);
	}

	public LoginError loginError() {
		return (LoginError) session.getAttribute(LOGIN_ERROR);
	}

	// ---- 画面に表示する記録 ------------------------------------------------------------------

	public List<Step> steps() {
		return listOf(STEPS);
	}

	public List<CallResult> calls() {
		return listOf(CALLS);
	}

	public void clearTrace() {
		steps().clear();
		calls().clear();
	}

	// RPのセッションを終了する(別のユーザーでログインし直せるようにする)
	public void invalidate() {
		session.invalidate();
	}

	@SuppressWarnings("unchecked")
	private <T> T get(String key) {
		return (T) session.getAttribute(key);
	}

	private <T> List<T> listOf(String key) {
		List<T> list = get(key);
		if (list == null) {
			list = new ArrayList<>();
			session.setAttribute(key, list);
		}
		return list;
	}
}
