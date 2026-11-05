package jp.sios.apisl.handson.keycloak.ressrv;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// 「スコープ」と「ロール」の組み合わせごとの結果を確かめる
@SpringBootTest
class AccessControlTests {

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static SimpleGrantedAuthority scope(String name) {
		return new SimpleGrantedAuthority("SCOPE_" + name);
	}

	private static SimpleGrantedAuthority role(String name) {
		return new SimpleGrantedAuthority("ROLE_" + name);
	}

	@Test
	void noToken_401() throws Exception {
		mvc.perform(get("/apis/whoami"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("unauthorized"));
	}

	@Test
	void playersNeedScopeOnly() throws Exception {
		mvc.perform(get("/apis/sports/baseball/players")
				.with(jwt().authorities(scope("read:baseball.player"))))
			.andExpect(status().isOk());
	}

	@Test
	void playersWithoutScope_403() throws Exception {
		mvc.perform(get("/apis/sports/baseball/players")
				.with(jwt().authorities(scope("read:soccer.player"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("forbidden"));
	}

	@Test
	void managersNeedScopeAndRole() throws Exception {
		mvc.perform(get("/apis/sports/baseball/managers")
				.with(jwt().authorities(scope("read:baseball.manager"), role("my-admin-role"))))
			.andExpect(status().isOk());
	}

	@Test
	void managersWithScopeButWithoutRole_403() throws Exception {
		mvc.perform(get("/apis/sports/baseball/managers")
				.with(jwt().authorities(scope("read:baseball.manager"), role("my-staff-role"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void managersWithRoleButWithoutScope_403() throws Exception {
		mvc.perform(get("/apis/sports/baseball/managers")
				.with(jwt().authorities(scope("read:baseball.player"), role("my-admin-role"))))
			.andExpect(status().isForbidden());
	}
}
