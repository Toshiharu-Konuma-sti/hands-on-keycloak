package jp.sios.apisl.handson.keycloak.rp.model;

import java.util.List;

// 画面の「結果」と「履歴」に表示する、API呼び出し1回分の記録
public record CallResult(int seq, String method, String path, int status, String message,
		String required, List<String> grantedScopes, List<String> grantedRoles, String body) {

	public String statusClass() {
		return status >= 200 && status < 300 ? "ok" : "ng";
	}
}
