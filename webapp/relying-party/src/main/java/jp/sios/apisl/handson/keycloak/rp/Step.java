package jp.sios.apisl.handson.keycloak.rp;

// 画面の「通信のトレース」に表示する1ステップ分の記録
// screen は、リクエストと応答の間に出す「画面が表示されたか」の説明(使わないステップは null)
public record Step(String title, String note, String request, String screen, String response) {

	public Step(String title, String note, String request, String response) {
		this(title, note, request, null, response);
	}
}
