package jp.sios.apisl.handson.keycloak.rp.model;

// 画面の「通信のトレース」に表示する1ステップ分の記録
// screen は、リクエストと応答の間に出す一言の補足(使わないステップは null)
// requestLabel / responseLabel は、2つの欄の見出し。通信ではないステップだけが指定する(null なら「リクエスト」「レスポンス」)
public record Step(String title, String note, String request, String screen, String response,
		String requestLabel, String responseLabel) {

	public Step(String title, String note, String request, String screen, String response) {
		this(title, note, request, screen, response, null, null);
	}

	public Step(String title, String note, String request, String response) {
		this(title, note, request, null, response);
	}
}
