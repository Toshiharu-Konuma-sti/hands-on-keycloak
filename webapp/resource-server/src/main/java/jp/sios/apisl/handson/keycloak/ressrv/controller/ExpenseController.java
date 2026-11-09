package jp.sios.apisl.handson.keycloak.ressrv.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jp.sios.apisl.handson.keycloak.ressrv.model.Expense;
import jp.sios.apisl.handson.keycloak.ressrv.model.NewExpense;
import jp.sios.apisl.handson.keycloak.ressrv.service.ExpenseService;

// 経費精算API。許可の条件は、各メソッドの@PreAuthorizeに書いてある。
//   SCOPE_xxx : アプリ(RP)に許された操作。トークンの`scope`クレーム
//   ROLE_xxx  : ユーザーに与えられた役割。トークンの`realm_access.roles`クレーム
// トークンの中身を読むのはこのクラスだけ。経費の処理は、ユーザー名を渡してExpenseServiceに任せる。
@RestController
@RequestMapping("/apis")
public class ExpenseController {

	private final ExpenseService expenses;

	public ExpenseController(ExpenseService expenses) {
		this.expenses = expenses;
	}

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

	// 読むスコープ と 一般社員以上のロールで許可する。自分が申請した経費だけを返す
	@GetMapping("/expenses")
	@PreAuthorize("hasAuthority('SCOPE_expenses:read') and hasRole('my-staff-role')")
	public List<Expense> myExpenses(@AuthenticationPrincipal Jwt jwt) {
		return expenses.listOf(jwt.getClaimAsString("preferred_username"));
	}

	// 申請には、書き込みのスコープ と 一般社員以上のロールが要る
	@PostMapping("/expenses")
	@PreAuthorize("hasAuthority('SCOPE_expenses:write') and hasRole('my-staff-role')")
	public ResponseEntity<Expense> claim(@RequestBody NewExpense body, @AuthenticationPrincipal Jwt jwt) {
		if (body.title() == null || body.title().isBlank() || body.amount() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titleと、1以上のamountが必要");
		}
		Expense created = expenses.claim(jwt.getClaimAsString("preferred_username"), body.title(), body.amount());
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// 承認に関するAPIも、スコープ と ロール の両方を満たしたときだけ許可する。
	//   承認待ち・承認済みの一覧: 「読む」スコープ(expenses:read)と、課長以上のロール
	//   経費の承認    : 「承認」スコープ(expenses:approve)と、部長のロール
	// ロールは「一般社員(my-staff-role) < 課長(my-manager-role) < 部長(my-admin-role)」の3段階。
	// 上位のロールは下位を含む(Keycloakの複合ロール)ため、部長のトークンにはすべてのロールが入っている。
	@GetMapping("/expenses/pending")
	@PreAuthorize("hasAuthority('SCOPE_expenses:read') and hasRole('my-manager-role')")
	public List<Expense> pending() {
		return expenses.pending();
	}

	@PostMapping("/expenses/{id}/approve")
	@PreAuthorize("hasAuthority('SCOPE_expenses:approve') and hasRole('my-admin-role')")
	public Expense approve(@PathVariable int id, @AuthenticationPrincipal Jwt jwt) {
		return expenses.approve(id, jwt.getClaimAsString("preferred_username"))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "経費が見つからない: " + id));
	}

	// 承認結果を確かめるための一覧。承認待ちの一覧と同じ条件で許可する。承認済みの経費を申請者を問わず返す
	@GetMapping("/expenses/approved")
	@PreAuthorize("hasAuthority('SCOPE_expenses:read') and hasRole('my-manager-role')")
	public List<Expense> approved() {
		return expenses.approved();
	}
}
