package jp.sios.apisl.handson.keycloak.rp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yaml の handson.* の設定値
@ConfigurationProperties("handson")
public record RpProperties(Keycloak keycloak, String resourceServerUrl, String redirectUri, String clientSecret) {

	// ブラウザが開くURLと、このアプリ(コンテナ)が直接呼ぶURLは、ホスト名が異なる
	public record Keycloak(String browserUrl, String serverUrl) {
	}
}
