package jp.sios.apisl.handson.keycloak.rp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import jp.sios.apisl.handson.keycloak.rp.model.Api;
import jp.sios.apisl.handson.keycloak.rp.model.ClientInfo;

// 画面で選べるもの(アプリ、スコープ、呼べるAPI)の一覧
@Component
public class Catalog {

	private static final List<ClientInfo> CLIENTS = List.of(
		new ClientInfo("app-expense-claim", "経費申請アプリ"),
		new ClientInfo("app-expense-approval", "経費承認アプリ"));

	// 画面で選べるoptional scope。どのアプリでも自由に要求できる。
	// アプリに割り当てていないスコープを要求すると、Keycloakがエラー(invalid_scope)で拒否する。
	// (expenses:read はdefault scopeなので、要求しなくても常に付く)
	private static final List<String> OPTIONAL_SCOPES = List.of("expenses:write", "expenses:approve");

	private static final List<Api> APIS = List.of(
		new Api("whoami", "GET", "/apis/whoami", "認証のみ", null),
		new Api("経費の一覧(自分の分)", "GET", "/apis/expenses", "スコープ expenses:read", null),
		new Api("経費を申請する", "POST", "/apis/expenses", "スコープ expenses:write",
			"{\"title\":\"出張宿泊費\",\"amount\":9800}"),
		new Api("承認待ちの一覧", "GET", "/apis/expenses/pending", "スコープ expenses:read かつ ロール my-manager-role(課長以上)", null),
		new Api("経費を承認する", "POST", "/apis/expenses/{id}/approve", "スコープ expenses:approve かつ ロール my-admin-role(部長)", null));

	public List<ClientInfo> clients() {
		return CLIENTS;
	}

	public List<String> optionalScopes() {
		return OPTIONAL_SCOPES;
	}

	public List<Api> apis() {
		return APIS;
	}

	// 一覧にないアプリが指定されたときは、先頭のアプリにする
	public ClientInfo clientOrDefault(String id) {
		return CLIENTS.stream().filter(c -> c.id().equals(id)).findFirst().orElse(CLIENTS.get(0));
	}

	// 一覧にあるスコープだけを、一覧の順で残す
	public List<String> chooseOptionalScopes(List<String> wanted) {
		return OPTIONAL_SCOPES.stream().filter(s -> wanted != null && wanted.contains(s)).toList();
	}

	public Optional<Api> findApi(String method, String path) {
		return APIS.stream().filter(a -> a.method().equals(method) && a.path().equals(path)).findFirst();
	}
}
