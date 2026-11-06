package jp.sios.apisl.handson.keycloak.rp.model;

// Keycloakに登録済みのクライアント(アプリ)。
// どのスコープを許すかはクライアントごとにKeycloak側で決まっており、RPはここでは判断しない。
public record ClientInfo(String id, String label) {
}
