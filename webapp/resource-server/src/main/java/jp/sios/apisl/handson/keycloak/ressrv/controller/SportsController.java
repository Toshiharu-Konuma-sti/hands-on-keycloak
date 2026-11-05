package jp.sios.apisl.handson.keycloak.ressrv.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 許可の条件は、各メソッドの@PreAuthorizeに書いてある。
//   SCOPE_xxx : アプリ(RP)に許された操作。トークンの`scope`クレーム
//   ROLE_xxx  : ユーザーに与えられた役割。トークンの`realm_access.roles`クレーム
@RestController
@RequestMapping("/apis")
public class SportsController {

	// 認証だけ必要。トークンの中身をそのまま返す
	@GetMapping("/whoami")
	public Map<String, Object> whoami(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
		Map<String, Object> me = new LinkedHashMap<>();
		me.put("username", jwt.getClaimAsString("preferred_username"));
		me.put("audience", jwt.getAudience());
		me.put("scopes", jwt.getClaimAsString("scope"));
		me.put("authorities", auth.getAuthorities().stream().map(Object::toString).toList());
		me.put("expires_at", jwt.getExpiresAt());
		return me;
	}

	// スコープだけで許可する
	@GetMapping("/sports/baseball/players")
	@PreAuthorize("hasAuthority('SCOPE_read:baseball.player')")
	public List<Map<String, String>> baseballPlayers() {
		return names("Lars Nootbaar", "Kensuke Kondo", "Shohei Ohtani", "Munetaka Murakami",
			"Masataka Yoshida", "Kazuma Okamoto", "Shugo Maki", "Sosuke Genda", "Yu Darvish");
	}

	// スコープ と ロール の両方を満たしたときだけ許可する
	@GetMapping("/sports/baseball/managers")
	@PreAuthorize("hasAuthority('SCOPE_read:baseball.manager') and hasRole('my-admin-role')")
	public List<Map<String, String>> baseballManagers() {
		return names("Sadaharu Oh", "Senichi Hoshino", "Tatsunori Hara", "Koji Yamamoto",
			"Hiroki Kokubo", "Atsunori Inaba", "Hideki Kuriyama");
	}

	@GetMapping("/sports/soccer/players")
	@PreAuthorize("hasAuthority('SCOPE_read:soccer.player')")
	public List<Map<String, String>> soccerPlayers() {
		return names("Shuichi Gonda", "Shogo Taniguchi", "Koh Itakura", "Yuto Nagatomo",
			"Maya Yoshida", "Takefusa Kubo", "Hidemasa Morita", "Junya Ito",
			"Daichi Kamada", "Ao Tanaka", "Daizen Maeda");
	}

	@GetMapping("/sports/soccer/managers")
	@PreAuthorize("hasAuthority('SCOPE_read:soccer.manager') and hasRole('my-admin-role')")
	public List<Map<String, String>> soccerManagers() {
		return names("Takeshi Okada", "Philippe Troussier", "Zico", "Alberto Zaccheroni",
			"Akira Nishino", "Hajime Moriyasu");
	}

	private static List<Map<String, String>> names(String... names) {
		var list = new java.util.ArrayList<Map<String, String>>();
		for (int i = 0; i < names.length; i++) {
			list.add(Map.of("id", String.valueOf(i + 1), "name", names[i]));
		}
		return list;
	}
}
