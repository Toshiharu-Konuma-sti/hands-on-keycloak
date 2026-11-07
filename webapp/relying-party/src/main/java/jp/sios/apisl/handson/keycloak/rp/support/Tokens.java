package jp.sios.apisl.handson.keycloak.rp.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

// トークンの読み取りと、PKCE/state用の乱数など、フローの本筋ではない補助処理
public final class Tokens {

	private static final SecureRandom RANDOM = new SecureRandom();

	private Tokens() {
	}

	// JWTのペイロード(2つ目の部分)をBase64URLで復号して、クレームのMapにする。署名は検証しない
	@SuppressWarnings("unchecked")
	public static Map<String, Object> claimsOf(JsonMapper json, String jwt) {
		String payload = jwt.split("\\.")[1];
		String text = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
		return json.readValue(text, Map.class);
	}

	// `aud`は文字列のことも配列のこともあるため、リストにそろえる
	public static List<String> audienceOf(Map<String, Object> claims) {
		Object aud = claims.get("aud");
		if (aud instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		return aud == null ? List.of() : List.of(String.valueOf(aud));
	}

	// トークンが発行されたアプリ(クライアント)。Keycloakのトークンでは`azp`クレームに入る
	public static String authorizedPartyOf(Map<String, Object> claims) {
		Object azp = claims.get("azp");
		return azp == null ? null : String.valueOf(azp);
	}

	public static String random(int bytes) {
		byte[] b = new byte[bytes];
		RANDOM.nextBytes(b);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
	}

	// PKCEのcode_challenge = BASE64URL(SHA-256(code_verifier))
	public static String s256(String verifier) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	// 長いトークンを、トレース表示用に短くする
	public static String abbreviate(String token) {
		return token.length() <= 24 ? token : token.substring(0, 24) + "...(" + token.length() + "文字)";
	}
}
