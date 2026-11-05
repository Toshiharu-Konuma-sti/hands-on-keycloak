package jp.sios.apisl.handson.keycloak.rp;

// 画面の「通信のトレース」に表示する1ステップ分の記録
public record Step(String title, String note, String request, String response) {
}
