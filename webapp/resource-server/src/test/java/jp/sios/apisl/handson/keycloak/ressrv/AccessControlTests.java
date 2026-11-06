package jp.sios.apisl.handson.keycloak.ressrv;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
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

	// スコープ(SCOPE_)とロール(ROLE_)を持つ、指定ユーザーのトークン
	private static JwtRequestPostProcessor token(String user, String... authorities) {
		var granted = java.util.Arrays.stream(authorities)
			.<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
		return jwt().jwt(j -> j.claim("preferred_username", user)).authorities(granted);
	}

	@Test
	void noToken_401() throws Exception {
		mvc.perform(get("/apis/expenses"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("unauthorized"));
	}

	@Test
	void read_needsScopeOnly_andReturnsOwnExpenses() throws Exception {
		mvc.perform(get("/apis/expenses").with(token("staff.sios", "SCOPE_expenses:read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].applicant", everyItem(is("staff.sios"))));
	}

	@Test
	void readWithoutScope_403() throws Exception {
		mvc.perform(get("/apis/expenses").with(token("staff.sios", "SCOPE_expenses:write")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("forbidden"));
	}

	@Test
	void claim_needsWriteScope() throws Exception {
		String body = "{\"title\":\"タクシー代\",\"amount\":2100}";
		mvc.perform(post("/apis/expenses").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)
				.with(token("staff.sios", "SCOPE_expenses:read", "SCOPE_expenses:write")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("申請中"));
		mvc.perform(post("/apis/expenses").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)
				.with(token("staff.sios", "SCOPE_expenses:read")))
			.andExpect(status().isForbidden());
	}

	@Test
	void pending_needsScopeAndManagerRole() throws Exception {
		// 課長のトークン: 課長と平社員のロール
		mvc.perform(get("/apis/expenses/pending")
				.with(token("taro.sios", "SCOPE_expenses:approve", "ROLE_my-manager-role", "ROLE_my-staff-role")))
			.andExpect(status().isOk());
		// 部長のトークン: すべてのロール(複合ロールで展開済み)
		mvc.perform(get("/apis/expenses/pending")
				.with(token("admin.sios", "SCOPE_expenses:approve",
					"ROLE_my-admin-role", "ROLE_my-manager-role", "ROLE_my-staff-role")))
			.andExpect(status().isOk());
	}

	@Test
	void pendingWithScopeButAsStaff_403() throws Exception {
		mvc.perform(get("/apis/expenses/pending")
				.with(token("staff.sios", "SCOPE_expenses:approve", "ROLE_my-staff-role")))
			.andExpect(status().isForbidden());
	}

	@Test
	void pendingWithManagerRoleButWithoutScope_403() throws Exception {
		mvc.perform(get("/apis/expenses/pending")
				.with(token("taro.sios", "SCOPE_expenses:read", "ROLE_my-manager-role", "ROLE_my-staff-role")))
			.andExpect(status().isForbidden());
	}

	@Test
	void approve_needsScopeAndAdminRole() throws Exception {
		mvc.perform(post("/apis/expenses/1/approve").with(csrf())
				.with(token("admin.sios", "SCOPE_expenses:approve",
					"ROLE_my-admin-role", "ROLE_my-manager-role", "ROLE_my-staff-role")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("承認済み"))
			.andExpect(jsonPath("$.approvedBy").value("admin.sios"));
	}

	@Test
	void approveAsManager_403() throws Exception {
		// 課長は一覧は見られるが、承認はできない
		mvc.perform(post("/apis/expenses/1/approve").with(csrf())
				.with(token("taro.sios", "SCOPE_expenses:approve", "ROLE_my-manager-role", "ROLE_my-staff-role")))
			.andExpect(status().isForbidden());
	}
}
