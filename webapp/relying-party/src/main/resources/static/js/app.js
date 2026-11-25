// アプリ(client_id)を切り替えたときは、要求するoptionalスコープのチェックをすべて外す
document.addEventListener('change', (event) => {
	if (!event.target.matches('input[type=radio][name=client]')) return;
	document.querySelectorAll('input[type=checkbox][name=scope]').forEach((box) => { box.checked = false; });
});

// トークンのペイロードから、そのトークンを得た通信(トレースの該当ステップ)へ移動し、開いて見せる。
// トレースを消去した後などで該当ステップが無いときは、トレース欄の先頭へ移動する
document.addEventListener('click', (event) => {
	const link = event.target.closest('a.trace-link');
	if (!link) return;
	event.preventDefault();
	const step = document.querySelector(link.getAttribute('href'));
	if (step) step.open = true;
	(step || document.getElementById('trace-section'))?.scrollIntoView({ behavior: 'smooth', block: 'start' });
});

// API呼び出しとトレース消去は、ページ全体を読み直さず、結果欄とトレースだけを差し替える。
// (ログイン/ログアウトはセッションが変わるため、従来どおりページ全体を読み直す)
document.addEventListener('submit', async (event) => {
	const form = event.target.closest('form.api-form');
	if (!form) return;
	event.preventDefault();
	const button = form.querySelector('button');
	button.disabled = true;
	try {
		const res = await fetch(form.action, {
			method: 'POST',
			headers: { 'X-Requested-With': 'fetch' },
			body: new URLSearchParams(new FormData(form)),
		});
		const doc = new DOMParser().parseFromString(await res.text(), 'text/html');
		const fresh = ['api-result', 'trace'].map((id) => doc.getElementById(id));
		if (fresh.includes(null)) {
			location.reload();   // セッション切れなど。画面全体を読み直す
			return;
		}
		fresh.forEach((node) => document.getElementById(node.id).replaceWith(node));
	} catch (e) {
		form.submit();       // fetchが使えないときは、従来どおりフォーム送信する
	} finally {
		button.disabled = false;
	}
});
