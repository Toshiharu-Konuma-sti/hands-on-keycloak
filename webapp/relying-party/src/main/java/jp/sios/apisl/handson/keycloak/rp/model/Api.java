package jp.sios.apisl.handson.keycloak.rp.model;

// 画面から呼べるResource ServerのAPI。ruleは画面に表示する「必要な条件」の説明(判断はRSが行う)
public record Api(String label, String method, String path, String rule, String body) {

	public boolean needsId() {
		return path.contains("{id}");
	}
}
