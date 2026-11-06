package jp.sios.apisl.handson.keycloak.rp.model;

// ログインに失敗してKeycloakから戻ってきたときの理由
public record LoginError(String error, String description, String hint) {
}
