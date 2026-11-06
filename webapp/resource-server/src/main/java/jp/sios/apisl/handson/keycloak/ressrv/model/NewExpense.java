package jp.sios.apisl.handson.keycloak.ressrv.model;

// 経費の申請(POST /apis/expenses)のリクエスト本文
public record NewExpense(String title, int amount) {
}
