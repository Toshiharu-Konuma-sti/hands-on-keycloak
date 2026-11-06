package jp.sios.apisl.handson.keycloak.ressrv.model;

// 経費1件。statusは「申請中」か「承認済み」
public record Expense(int id, String applicant, String title, int amount, String status, String approvedBy) {
}
