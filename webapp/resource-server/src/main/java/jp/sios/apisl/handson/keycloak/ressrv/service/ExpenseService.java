package jp.sios.apisl.handson.keycloak.ressrv.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import jp.sios.apisl.handson.keycloak.ressrv.model.Expense;

// 経費のデータを扱う。トークンや認可のことは知らず、ユーザー名は引数で受け取る。
// データはメモリ上にあり、再起動すると初期状態に戻る。
@Service
public class ExpenseService {

	private final Map<Integer, Expense> expenses = new ConcurrentHashMap<>();
	private final AtomicInteger nextId = new AtomicInteger(1);

	public ExpenseService() {
		claim("staff.sios", "出張交通費(東京-大阪)", 14500);
		claim("staff.sios", "書籍代", 3200);
		claim("taro.sios", "取引先との会食費", 8600);
	}

	public List<Expense> listOf(String applicant) {
		return expenses.values().stream()
			.filter(e -> e.applicant().equals(applicant))
			.sorted(Comparator.comparingInt(Expense::id))
			.toList();
	}

	public List<Expense> pending() {
		return expenses.values().stream()
			.filter(e -> e.status().equals("申請中"))
			.sorted(Comparator.comparingInt(Expense::id))
			.toList();
	}

	public List<Expense> approved() {
		return expenses.values().stream()
			.filter(e -> e.status().equals("承認済み"))
			.sorted(Comparator.comparingInt(Expense::id))
			.toList();
	}

	public Expense claim(String applicant, String title, int amount) {
		Expense e = new Expense(nextId.getAndIncrement(), title, amount, "申請中", applicant, null);
		expenses.put(e.id(), e);
		return e;
	}

	// 該当の経費が無いときは空を返す
	public Optional<Expense> approve(int id, String approver) {
		Expense target = expenses.get(id);
		if (target == null) {
			return Optional.empty();
		}
		Expense approved = new Expense(target.id(), target.title(), target.amount(),
			"承認済み", target.applicant(), approver);
		expenses.put(id, approved);
		return Optional.of(approved);
	}
}
