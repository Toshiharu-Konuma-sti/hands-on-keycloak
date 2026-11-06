package jp.sios.apisl.handson.keycloak.ressrv.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

// 経費精算API。許可の条件は、各メソッドの@PreAuthorizeに書いてある。
//   SCOPE_xxx : アプリ(RP)に許された操作。トークンの`scope`クレーム
//   ROLE_xxx  : ユーザーに与えられた役割。トークンの`realm_access.roles`クレーム
// 経費のデータはメモリ上にあり、再起動すると初期状態に戻る。
@RestController
@RequestMapping("/apis")
public class ExpenseController {

	public record Expense(int id, String applicant, String title, int amount, String status, String approvedBy) {
	}

	public record NewExpense(String title, int amount) {
	}

	private final Map<Integer, Expense> expenses = new ConcurrentHashMap<>();
	private final AtomicInteger nextId = new AtomicInteger(1);

	public ExpenseController() {
		add("staff.sios", "出張交通費(東京-大阪)", 14500);
		add("staff.sios", "書籍代", 3200);
		add("taro.sios", "取引先との会食費", 8600);
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

	// スコープだけで許可する。自分が申請した経費だけを返す
	@GetMapping("/expenses")
	@PreAuthorize("hasAuthority('SCOPE_expenses:read')")
	public List<Expense> myExpenses(@AuthenticationPrincipal Jwt jwt) {
		String me = jwt.getClaimAsString("preferred_username");
		return expenses.values().stream()
			.filter(e -> e.applicant().equals(me))
			.sorted(Comparator.comparingInt(Expense::id))
			.toList();
	}

	// 申請には、書き込みのスコープが要る
	@PostMapping("/expenses")
	@PreAuthorize("hasAuthority('SCOPE_expenses:write')")
	public ResponseEntity<Expense> claim(@RequestBody NewExpense body, @AuthenticationPrincipal Jwt jwt) {
		if (body.title() == null || body.title().isBlank() || body.amount() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titleと、1以上のamountが必要");
		}
		Expense created = add(jwt.getClaimAsString("preferred_username"), body.title(), body.amount());
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// 承認に関するAPIは、スコープ と ロール の両方を満たしたときだけ許可する。
	// ロールは「平社員(my-staff-role) < 課長(my-manager-role) < 部長(my-admin-role)」の3段階。
	// 上位のロールは下位を含む(Keycloakの複合ロール)ため、部長のトークンにはすべてのロールが入っている。
	// 承認待ちの一覧は課長以上、経費の承認は部長だけ。
	@GetMapping("/expenses/pending")
	@PreAuthorize("hasAuthority('SCOPE_expenses:approve') and hasRole('my-manager-role')")
	public List<Expense> pending() {
		return expenses.values().stream()
			.filter(e -> e.status().equals("申請中"))
			.sorted(Comparator.comparingInt(Expense::id))
			.toList();
	}

	@PostMapping("/expenses/{id}/approve")
	@PreAuthorize("hasAuthority('SCOPE_expenses:approve') and hasRole('my-admin-role')")
	public Expense approve(@PathVariable int id, @AuthenticationPrincipal Jwt jwt) {
		Expense target = expenses.get(id);
		if (target == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "経費が見つからない: " + id);
		}
		Expense approved = new Expense(target.id(), target.applicant(), target.title(), target.amount(),
			"承認済み", jwt.getClaimAsString("preferred_username"));
		expenses.put(id, approved);
		return approved;
	}

	private Expense add(String applicant, String title, int amount) {
		Expense e = new Expense(nextId.getAndIncrement(), applicant, title, amount, "申請中", null);
		expenses.put(e.id(), e);
		return e;
	}
}
