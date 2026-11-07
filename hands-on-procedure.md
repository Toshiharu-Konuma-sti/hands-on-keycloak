
# 簡易手順メモ

## 目次

- [SAML/OIDC連携の演習](#samloidc連携の演習)
    - [前提(事前準備)](#前提事前準備)
    - [NextcloudでSAML連携](#nextcloudでsaml連携)
        - [1. NextcloudのSP情報を確認する](#1-nextcloudのsp情報を確認する)
        - [2. KeycloakにSAMLクライアントを作成する](#2-keycloakにsamlクライアントを作成する)
        - [3. NextcloudにIdP(Keycloak)の情報を設定する](#3-nextcloudにidpkeycloakの情報を設定する)
        - [4. 動作確認](#4-動作確認)
        - [5. (発展)確認ポイント](#5-発展確認ポイント)
        - [トラブルシュート](#トラブルシュート)
    - [GrafanaでOIDC連携](#grafanaでoidc連携)
        - [1. KeycloakにOIDCクライアントを作成する](#1-keycloakにoidcクライアントを作成する)
        - [2. GrafanaにOIDCの設定をする](#2-grafanaにoidcの設定をする)
        - [3. 動作確認](#3-動作確認)
        - [4. KeycloakのロールをGrafanaのロールにマッピングする](#4-keycloakのロールをgrafanaのロールにマッピングする)
        - [5. (発展)確認ポイント](#5-発展確認ポイント-1)
        - [トラブルシュート](#トラブルシュート-1)
- [RP と RS の連携(IdP → RP → RS)の体験](#rp-と-rs-の連携idp--rp--rsの体験)
    - [前提](#前提)
    - [スコープとロール](#スコープとロール)
    - [体験1: ログインして、トークンの中身を見る](#体験1-ログインしてトークンの中身を見る)
    - [体験2: 一覧を見て、申請してみる(スコープ)](#体験2-一覧を見て申請してみるスコープ)
    - [体験3: スコープを増やして、申請する](#体験3-スコープを増やして申請する)
    - [体験4: 課長と部長で、承認待ちの一覧と承認を試す(スコープとロール)](#体験4-課長と部長で承認待ちの一覧と承認を試すスコープとロール)
    - [体験5: アプリの違いを見る(Keycloakがスコープを決める)](#体験5-アプリの違いを見るkeycloakがスコープを決める)
    - [まとめ(期待される結果)](#まとめ期待される結果)
    - [興味のある人向け: 仕組みを読む](#興味のある人向け-仕組みを読む)
    - [トラブルシュート](#トラブルシュート-2)


## SAML/OIDC連携の演習

### 前提(事前準備)

- コンテナが起動していること。
    $ cd container && ./CREATE_CONTAINER.sh
- Keycloakに`handson`レルムとユーザ等が作成済みであること。
    $ cd setup && ./SETUP_HANDS-ON.sh
- 各サービスのURLと認証情報。

    | サービス  | URL                    | ユーザ / パスワード                     |
    |-----------|------------------------|-----------------------------------------|
    | Keycloak  | http://localhost:8080/ | admin / password (masterレルム管理者)   |
    | Nextcloud | http://localhost:8081/ | admin / password (ローカル管理者)       |
    | Grafana   | http://localhost:3000/ | admin / admin    (ローカル管理者)       |

- 連携後にログインするKeycloakのユーザ(`handson`レルム)。

    | ユーザ      | パスワード  | 役職   | 付与されるレルムロール                                          |
    |-------------|-------------|--------|-----------------------------------------------------------------|
    | staff.sios  | staff.sios  | 一般社員 | my-staff-role                                                   |
    | taro.sios   | taro.sios   | 課長   | my-manager-role (my-staff-roleを含む)                           |
    | admin.sios  | admin.sios  | 部長   | my-admin-role (my-manager-roleとmy-staff-roleを含む)             |

    上位のロールは下位のロールを含む(複合ロール)。トークンには、含まれるロールもすべて展開されて入る。

- ブラウザからのアクセスは`localhost`、コンテナ間(サーバ間)の通信はコンテナ名(`keycloak`)を使う点に注意する。


### NextcloudでSAML連携

Keycloakが IdP(Identity Provider)、Nextcloudが SP(Service Provider) になる。
Nextcloudはコンテナ初回起動時に自動インストールされ、`user_saml`アプリ(8.5.0)も導入済み。

#### 1. NextcloudのSP情報を確認する

1. http://localhost:8081/ に`admin / password`でログインする。
2. 右上のアイコン > [アプリ] > [有効なアプリ] に`SSO & SAML authentication`(user_saml)があることを確認する。
   - 無い場合は、以下で導入する。(または[アプリ]の検索から有効化する)
       $ docker exec -u www-data nextcloud php occ app:install user_saml
3. 右上のアイコン > [管理者設定] > 左メニュー[SSOとSAML認証]を開く。
   ここにはSPの値は表示されない。[組み込みのSAML認証を使用する]/[環境変数を使用する]の2つのボタンだけが表示されるのが正しい。
   (プロバイダーの登録は手順3で行う)
4. 以下のSP(Nextcloud)の値を控える。(後でKeycloakに登録する。この画面には表示されないが、何も入力しなければ既定でこの値になる)
    - Service Provider Entity ID            : http://localhost:8081/apps/user_saml/saml/metadata
    - Assertion Consumer Service URL (ACS)  : http://localhost:8081/apps/user_saml/saml/acs
   ※上記のmetadata URLは、手順3でプロバイダーを登録して保存するまでは`SAML provider #1 does not exist or has not been configured yet.`のエラーになる。

#### 2. KeycloakにSAMLクライアントを作成する

1. http://localhost:8080/admin/ に`admin / password`でログインし、`handson`レルムを選択する。
2. [Clients] > [Create client] を押し、以下を入力して[Next]。
    - Client type : SAML
    - Client ID   : http://localhost:8081/apps/user_saml/saml/metadata  (NextcloudのEntity IDと完全一致させる)
    - Name        : nextcloud
3. 以下を入力して[Save]。
    - Root URL            : http://localhost:8081/
    - Valid redirect URIs : http://localhost:8081/apps/user_saml/saml/acs
4. [Settings]タブで以下を設定して[Save]。
    - Name ID format       : username
    - Force name ID format : On
    - Sign documents       : On (既定)
      ([Logout settings]には`Front channel logout`のスイッチだけがある。既定の On のままにする)
5. [Keys]タブ > `Client signature required`を Off にして[Save]。
   (Nextcloudは既定でSAMLリクエストに署名しないため)
6. [Advanced]タブ > [Fine Grain SAML Endpoint Configuration]を開き、以下を設定して、そのセクションの[Save]。
    - Logout Service Redirect Binding URL : http://localhost:8081/apps/user_saml/saml/sls
   (Nextcloudのシングルログアウト(SLO)用URL。未設定だとNextcloudのログアウト時に
    Keycloakで`ログアウトに失敗しました`と表示される)
7. [Client scopes]タブ > `<Client ID>-dedicated` > [Configure a new mapper] > [User Property] で、以下のマッパーを作成する。

    | Name     | Property  | SAML Attribute Name | SAML Attribute NameFormat |
    |----------|-----------|---------------------|---------------------------|
    | username | username  | username            | Basic                     |
    | email    | email     | email               | Basic                     |
    | name     | firstName | displayName         | Basic                     |

8. [Client scopes]タブで`role_list`を選択して[Remove]する。
   (`role_list`は既定で、ロールごとに同名(`Role`)のAttributeを複数出力する。
    Nextcloudのsaml処理は同名のAttributeを許さずエラーになるため。ロールは今回使わない)

#### 3. NextcloudにIdP(Keycloak)の情報を設定する

1. http://localhost:8080/realms/handson/protocol/saml/descriptor をブラウザで開き、以下の値を確認する。
    - `EntityDescriptor`の`entityID`                  : http://localhost:8080/realms/handson
    - `SingleSignOnService`(`HTTP-POST`)の`Location`  : http://localhost:8080/realms/handson/protocol/saml
    - `<ds:X509Certificate>`の値                      : 署名検証用の証明書
   ※メタデータURLからの自動取得は使わず、手入力する。
     Nextcloudコンテナからは`localhost:8080`に届かず、`keycloak:8080`で取得すると
     ブラウザから到達できないURLが設定に入ってしまうため。
2. Nextcloudの[管理者設定] > [SSOとSAML認証]で、[組み込みのSAML認証を使用する]を押す。
   (UIが英語の場合は`Use built-in SAML authentication`)
3. [グローバル設定]で`複数のユーザーのバックエンド（LDAPなど）の使用を許可する`にチェックを入れる。
   (外すとローカル管理者でログインできなくなるため、必ず入れる)
4. [登録済みのプロバイダー]の[プロバイダーの新規登録]を押し、作成された`Provider 1`の設定画面を開く。
   以下を設定する。(画面の見出しは[一般]/[Service Providerデータ]/[Identity Providerデータ]/[属性マッピング]/[セキュリティ設定])
    - [一般]
        - UIDをマップする属性              : username
          (「属性マッピング」ではなく、[一般]にある。必須項目)
    - [Service Providerデータ]
        - サービスプロバイダエンティティID(オプション) : 空欄のまま(既定で手順1-4のEntity IDになる)
    - [Identity Providerデータ]
        - IdPエンティティの識別子(URIでなければならない) : http://localhost:8080/realms/handson
        - SPが認証要求メッセージを送信するIdPのURLターゲット
                                           : http://localhost:8080/realms/handson/protocol/saml
        - URL SPがSLO要求を送信するIdPの場所 : http://localhost:8080/realms/handson/protocol/saml
        - IdPの公開X.509証明書             : 手順1の`X509Certificate`の値
          (Keycloak管理画面 > Realm settings > Keys > RS256 の[Certificate]でも取得できる)
    - [属性マッピング]
        - 表示名をにマップする属性         : displayName
        - 電子メールアドレスをマップする属性 : email
        - (クォータ/ホーム/グループ/MFA/LDAP/グループマッピング接頭辞は空欄のまま)
    - [セキュリティ設定]は既定のままにする。
5. [変更を保存]を押す。
6. 設定画面に`有効なメタデータ`と表示されることを確認する。(`メタデータが無効です`の場合は入力値を見直す)
   また、[メタデータXMLをダウンロード]で取得できるSPメタデータが、手順1-4のEntity ID/ACS URLになっていることを確認する。

#### 4. 動作確認

1. Nextcloudからログアウトし(またはシークレットウィンドウで) http://localhost:8081/ を開く。
2. ログイン画面の[SSO & SAML log in]を押す。
3. Keycloakのログイン画面に遷移することを確認し、`admin.sios / admin.sios`でログインする。
4. Nextcloudにログインでき、右上のユーザが`admin.sios`になっていることを確認する。
5. ローカル管理者でログインし直し、[ユーザ]一覧に`admin.sios`が自動作成されていることを確認する。
6. 他のユーザ(`staff.sios`、`taro.sios`)でも同様にログインできることを確認する。
7. Nextcloudの右上のアイコン > [ログアウト]を押し、Keycloakを経由してNextcloudのログイン画面に戻ることを確認する。
   (Keycloak管理画面 > Sessions で、該当ユーザのセッションが消えていることも確認する)

#### 5. (発展)確認ポイント

- Keycloak管理画面 > Clients > nextcloud > [Client scopes] > [Evaluate] で、ユーザを指定して生成される情報を確認する。
- Keycloak管理画面 > Sessions で、SAMLクライアントのセッションが作成されていることを確認する。
- SSOを経由せずローカル管理者でログインしたいときは http://localhost:8081/login?direct=1 を使う。

#### トラブルシュート

- Keycloakで`Invalid requester`/`Client not found`と表示される
    -> KeycloakのClient IDとNextcloudのEntity IDが一致していない。
- Keycloakで`Invalid redirect uri`と表示される
    -> `Valid redirect URIs`がACS URLと一致していない。
- Nextcloudでログインに失敗する(署名検証エラー等)
    -> IdPのX.509証明書の貼り付けミス、またはIdentifier of the IdPの値の誤りを確認する。
- ログインできたがユーザ名が意図しない値になる
    -> Name ID formatと、UIDの属性マッピング(username)、Keycloakのマッパーを見直す。
- Keycloakでログイン後、Nextcloudで`内部サーバーエラー`になる
    -> 画面のリクエストIDで、以下のようにログを確認する。
       $ docker exec nextcloud grep <リクエストID> /var/www/html/data/nextcloud.log
       `Found an Attribute element with duplicated Name`の場合は、手順2-8の`role_list`の除去ができていない。
- Nextcloudでログアウトすると、Keycloakで`ログアウトに失敗しました`と表示される
    -> 手順2-6の`Logout Service Redirect Binding URL`が未設定。
       (Keycloakのログに`Can't finish SAML logout as there is no logout binding set`が出る)
- [アプリ]に`SSO & SAML authentication`が表示されない
    -> 手順1-2の`occ app:install`で導入する。(フックが動かなかった場合は`docker compose logs nextcloud`で確認する)
- Keycloakでログイン後、Nextcloudのログイン画面に戻り、ログインとリダイレクトを繰り返す(ループする)
    -> `user_saml`が発行する`saml_data`クッキーが`SameSite=None`でSecure属性無し(HTTP)のため、Chromeに破棄されている。
       (NextcloudのACS処理が`Cookie was not present`となり、トップへ戻される)
       コンテナ作成時のフックで`SameSite=Lax`に変更済み。ループする場合は以下で変更し、Apacheを再読込する。
       (localhost同士はsame-siteのため、Laxでクッキーが送られる)
       $ docker exec nextcloud sh -c "sed -i \"s/\(saml_data', \\\$data, null, '\)None'/\1Lax'/\" custom_apps/user_saml/lib/Controller/SAMLController.php"
       $ docker exec nextcloud apache2ctl graceful
- ローカル管理者でログインできなくなった
    -> 以下でSAML設定を解除する。
       $ docker exec -u www-data nextcloud php occ config:app:set user_saml type --value=""


### GrafanaでOIDC連携

KeycloakがOP(OpenID Provider)、GrafanaがRP(Relying Party)になる。
Grafana側の設定は、管理画面(Generic OAuth)から行う。

#### 1. KeycloakにOIDCクライアントを作成する

1. http://localhost:8080/admin/ にログインし、`handson`レルムを選択する。
2. [Clients] > [Create client] を押し、以下を入力して[Next]。
    - Client type : OpenID Connect
    - Client ID   : grafana
    - Name        : grafana
3. [Capability config]で以下を設定して[Next]。
    - Client authentication : On
    - Standard flow         : On
    - (その他は既定のまま)
4. [Login settings]で以下を入力して[Save]。
    - Root URL            : http://localhost:3000/
    - Valid redirect URIs : http://localhost:3000/login/generic_oauth
    - Web origins         : http://localhost:3000
5. [Credentials]タブの`Client Secret`を控える。

#### 2. GrafanaにOIDCの設定をする

1. http://localhost:3000/ に`admin / admin`でログインする。(パスワード変更を求められたらスキップ可)
2. [Administration] > [Authentication] > [Generic OAuth] を開き、以下を入力する。
    - Enabled              : On
    - Display name         : Keycloak
    - Client ID            : grafana
    - Client secret        : 手順1-5で控えたClient Secret
    - Scopes               : openid email profile
                             (既定で入っている`user:email`は必ず削除する。Keycloakに存在しないスコープのため認証が拒否される)
    - Auth URL             : http://localhost:8080/realms/handson/protocol/openid-connect/auth
    - Token URL            : http://keycloak:8080/realms/handson/protocol/openid-connect/token
    - API URL              : http://keycloak:8080/realms/handson/protocol/openid-connect/userinfo
    - Allow sign up        : On
    - Login attribute path : preferred_username
    - Name attribute path  : name
    - Use PKCE             : On (任意)
   ※Auth URLはブラウザが開くため`localhost`、Token/API URLはGrafanaコンテナが呼ぶため`keycloak`にする。
3. [Save]を押す。

#### 3. 動作確認

1. Grafanaからログアウトし(またはシークレットウィンドウで) http://localhost:3000/login を開く。
2. [Sign in with Keycloak]を押す。
3. Keycloakのログイン画面に遷移し、`staff.sios / staff.sios`でログインする。
4. Grafanaにログインでき、ユーザが`staff.sios`になっていることを確認する。
5. この時点ではロール未設定のため、権限は既定の`Viewer`になる。

#### 4. KeycloakのロールをGrafanaのロールにマッピングする

Keycloakのレルムロールは、アクセストークンの`realm_access.roles`に含まれる。
これをGrafanaのロールに変換する。

1. Keycloak管理画面 > Clients > grafana > [Client scopes] > `grafana-dedicated` > [Scope]タブを開き、
   `Full scope allowed`が On であることを確認する。
2. Grafanaの[Administration] > [Authentication] > [Generic OAuth] を開き、以下を設定して[Save]。
    - Role attribute path :
      contains(realm_access.roles[*], 'my-admin-role') && 'Admin' || contains(realm_access.roles[*], 'my-staff-role') && 'Editor' || 'Viewer'
3. 一度ログアウトし、ユーザを替えて再ログインして、以下になることを確認する。(ロールはログイン時に同期される)
    - admin.sios : Admin   (my-admin-role)
    - staff.sios : Editor  (my-staff-role)
    - taro.sios  : Editor  (課長のmy-manager-roleと、含まれるmy-staff-roleは持つが、my-admin-roleは持たないため)
4. ロールは、ローカル管理者でログインして[Administration] > [Users and access] > [Users]で確認できる。

#### 5. (発展)確認ポイント

- Keycloak管理画面 > Clients > grafana > [Client scopes] > [Evaluate] で、ユーザを指定して生成されるアクセストークン/IDトークン/UserInfoの中身を確認する。
- http://localhost:8080/realms/handson/.well-known/openid-configuration で、Auth/Token/UserInfoの各エンドポイントを確認する。
- Keycloakのユーザからレルムロール(またはグループ)を外し、再ログイン後のGrafanaのロールが変わることを確認する。

#### トラブルシュート

- Keycloakで`Invalid parameter: redirect_uri`と表示される
    -> `Valid redirect URIs`が`http://localhost:3000/login/generic_oauth`になっていない。
- Grafanaで`Login failed` / `Login provider denied login request`となり、Keycloakの画面に遷移しない
    -> Scopesに`user:email`(Grafanaの既定値)が残っている。削除して`openid email profile`だけにする。
       (`docker logs grafana`に`invalid_scope`/`Invalid scopes: user:email ...`が出る)
- Grafanaでログイン後にトークン取得エラーになる
    -> Token URLのホスト名が`keycloak`でなく`localhost`になっていないか確認する。(コンテナ内の`localhost`はGrafana自身を指す)
- Grafanaでクライアント認証エラーになる
    -> Client Secretの値を確認する。
- Grafanaで`Email is required`等のエラーになる
    -> Scopesに`email`が含まれているか確認する。
- ロールが常に`Viewer`になる
    -> Role attribute pathの記述(`my-admin-role`等)と、Keycloakのユーザにロールが付与されているかを確認する。


## RP と RS の連携(IdP → RP → RS)の体験

SAMLやOIDCの設定を体験する演習とは違い、ここは「できあがった構成を動かして観察する」演習。
設定は不要。ブラウザで操作し、画面に表示される通信の内容を見ながら、スコープとロールの違いを理解する。
題材は「経費精算」。一般社員が経費を申請し、課長が承認待ちを確認し、部長が承認する。

```
 ブラウザ ──(1)ログイン──▶ Keycloak(IdP)
    │                          │
    │◀──(2)code を付けて戻る────┘
    ▼
 Relying Party(RP) ──(3)codeをトークンに交換──▶ Keycloak
    │
    └──(5)アクセストークンを付けて API を呼ぶ──▶ Resource Server(RS)
                                                  └ トークンのスコープとロールで許可/拒否を判断
```

- RP: http://localhost:8222/ (Spring MVC + Thymeleaf。Authorization Code + PKCE を手書きで実装)
- RS: http://localhost:8111/ (Spring Security の Resource Server。JWTを検証して許可を判断)
- ソース: `webapp/` (Gradleのマルチプロジェクト。`relying-party`と`resource-server`)

### 前提

- 「前提(事前準備)」のとおり、コンテナの起動と`SETUP_HANDS-ON.sh`が完了していること。
- RPとRSは、コンテナの初回起動時にソースをビルドする。数分かかるため、以下が表示されるまで待つ。
    $ docker logs relying-party 2>&1 | grep "Started Application"
    $ docker logs resource-server 2>&1 | grep "Started Application"
  (Gradleのキャッシュは保存されるため、2回目以降は速い)
- RP/RSのコンテナを使うと、Keycloakの`KC_HOSTNAME`を`http://localhost:8080`に固定している。
  トークンの`iss`(発行者)が、ブラウザからでもコンテナからでも同じ値になる。

### スコープとロール

| 種類     | 意味                                       | トークン上の場所      | このハンズオンでの値                                                              |
|----------|--------------------------------------------|---------------------|-------------------------------------------------------------------------------|
| スコープ | アプリ(RP)に許した操作(ユーザーが同意する)   | `scope`             | `expenses:read`(既定) / `expenses:write` / `expenses:approve`(いずれも任意)   |
| ロール   | ユーザーに与えられた役割(管理者が付与する)   | `realm_access.roles`| `my-staff-role`(一般社員) / `my-manager-role`(課長) / `my-admin-role`(部長)    |

アプリ(Keycloakのクライアント)ごとに、付与できるスコープが違う。

| アプリ                                | 常に付与(default scope) | 要求したときだけ付与(optional scope) |
|-------------------------------------|---------------------------|------------------------------------|
| 経費申請アプリ (`app-expense-claim`)    | `expenses:read`           | `expenses:write`                   |
| 経費承認アプリ (`app-expense-approval`) | `expenses:read`           | `expenses:approve`                 |

画面では、optional scope(`expenses:write` / `expenses:approve`)を、アプリに関係なく自由に選んで要求できる。
ただし、そのアプリに割り当てていないスコープを要求すると、Keycloakは**ログインを拒否してエラー(`invalid_scope`)で戻す**。
どのスコープを付与するかは、RPではなくKeycloakのクライアント設定で決まる。

ユーザーと役職: `staff.sios`=一般社員、`taro.sios`=課長、`admin.sios`=部長。
上位のロールは下位のロールを含む(Keycloakの複合ロール)。部長のトークンには、`my-admin-role`だけでなく、含まれる`my-manager-role`と`my-staff-role`も入る。

| 役職   | ユーザー   | 付与されるロール(トークンに入るもの)                                |
|--------|------------|-----------------------------------------------------------------------|
| 一般社員 | staff.sios | `my-staff-role`                                                       |
| 課長   | taro.sios  | `my-manager-role`、`my-staff-role`                                    |
| 部長   | admin.sios | `my-admin-role`、`my-manager-role`、`my-staff-role`                   |

RSの各APIは、次の条件で許可を判断する。(`ExpenseController`の`@PreAuthorize`)

| API                                  | 必要な条件                                                              |
|--------------------------------------|---------------------------------------------------------------------|
| `GET  /apis/whoami`                  | 認証のみ(トークンの中身を返す)                                          |
| `GET  /apis/expenses`                | スコープ `expenses:read`(自分が申請した経費だけ返す)                     |
| `POST /apis/expenses`                | スコープ `expenses:write`(経費の申請)                                 |
| `GET  /apis/expenses/pending`        | スコープ `expenses:read` **かつ** ロール `my-manager-role`(課長以上)(承認待ちの一覧) |
| `POST /apis/expenses/{id}/approve`   | スコープ `expenses:approve` **かつ** ロール `my-admin-role`(部長のみ)(経費の承認)       |

- RSは、`aud`(宛先)に`expense-api`を含むトークンだけを受け付ける。宛先が違うトークンは401になる。
- 経費のデータはRSのメモリ上にあり、初期データは3件(staff.siosの2件、taro.siosの1件、いずれも申請中)。RSを再起動すると初期状態に戻る。

### 体験1: ログインして、トークンの中身を見る

1. http://localhost:8222/ を開く。
2. 最初、このRPはどのアプリでもない(画面に「いまのRP: このRPを振る舞わせるアプリを選択してログインしてください」と出ている)。
   アプリのラジオボタンで`経費申請アプリ`を選び、optionalスコープ(`expenses:write` / `expenses:approve`)は**どちらもチェックなし**のまま[ログイン]を押す。
   (ログインすると、このRPは、選んだアプリとして振る舞う。アプリを選んだだけでは、変わらない)
3. Keycloakのログイン画面で`staff.sios / staff.sios`(一般社員)でログインする。
4. 同意画面で、RPが要求している内容(「経費申請アプリ (app-expense-claim)へのアクセスを許可」と、「経費の参照 (expenses:read)」などのスコープ)を確認して[Yes]を押す。(スコープは「アプリに許すこと」)
5. RPに戻ったら、画面の以下を見る。
    - ページのタイトルが`Relying Party (RP)：経費申請アプリ`になり、背景が薄い青になる。このRPが、経費申請アプリとして振る舞っている。(経費承認アプリでログインすると、背景は薄い黄色になる)
    - [取得したトークンの中身]: ユーザー、メール、スコープ(`expenses:read`)、ロール(`my-staff-role`)、宛先(`expense-api`)
    - [通信のトレース]: 1〜4の各ステップの通信内容。
      認可リクエスト → codeを持って戻る → codeをトークンに交換 → IDトークンの検証

### 体験2: 一覧を見て、申請してみる(スコープ)

体験1のログインのまま、[APIを呼ぶ]を試す。
結果はボタンのすぐ下に表示される。ページは読み直されないため、スクロール位置はそのままで、何度でも続けて試せる。
(最新の結果と、これまでの呼び出しの履歴、下の「通信のトレース」が更新される)

1. `GET 経費の一覧(自分の分)`を押す。→ **200**。`expenses:read`があるため。staff.siosが申請した2件が返る。
2. `POST 経費を申請する`を押す。→ **403**。レスポンスの`required`と`granted`を見比べる。申請に必要な`expenses:write`が、このトークンには無い。
3. `GET 承認待ちの一覧`を押す。→ **403**。スコープ`expenses:read`はあるが、課長以上のロール`my-manager-role`が無い。

### 体験3: スコープを増やして、申請する

1. [ログアウト]を押す。(RPとKeycloakの両方のセッションが終了する)
2. `経費申請アプリ`、`expenses:write`に**チェックを入れて**、`staff.sios`でログインする。同意画面に「経費の申請 (expenses:write)」が増える。
3. `POST 経費を申請する`を押す。→ **201**。申請中の経費が作成される。
4. `GET 経費の一覧(自分の分)`を押す。→ **200**。申請した経費が増えている(3件)。

### 体験4: 課長と部長で、承認待ちの一覧と承認を試す(スコープとロール)

1. ログアウトし、`経費承認アプリ`、`expenses:approve`に**チェックを入れて**、`staff.sios`(一般社員)でログインする。
2. `GET 承認待ちの一覧`を押す。→ **403**。`granted`にスコープ`expenses:read`はあるが、ロール`my-manager-role`が無い。
   (アプリには承認を許しているが、ユーザーは課長以上ではない)
3. ログアウトし、同じアプリ、`expenses:approve`にチェックを入れて、`taro.sios`(課長)でログインする。
   [取得したトークンの中身]のロールに、`my-manager-role`と、含まれる`my-staff-role`が入っている。
4. `GET 承認待ちの一覧`を押す。→ **200**。申請中の経費が全員分返る。(課長以上は一覧を見られる)
5. `POST 経費を承認する`を、IDを`1`にして押す。→ **403**。`granted`のロールに`my-admin-role`が無い。
   (課長は一覧を見られるが、承認は部長だけ)
6. ログアウトし、同じアプリ、`expenses:approve`にチェックを入れて、`admin.sios`(部長)でログインする。
   [取得したトークンの中身]のロールに、`my-admin-role`、`my-manager-role`、`my-staff-role`の3つが入っている。(複合ロール)
7. `POST 経費を承認する`を、IDを`1`にして押す。→ **200**。`status`が`承認済み`、`approvedBy`が`admin.sios`になる。
8. `GET 承認待ちの一覧`を押すと、承認した経費が消えている。

### 体験5: アプリの違いを見る(Keycloakがスコープを決める)

スコープを付与するか拒否するかは、RPではなくKeycloakのクライアント設定で決まる。

1. ログアウトし、`経費申請アプリ`、`expenses:approve`にチェックを入れて、[ログイン]を押す。
2. Keycloakのログイン画面は出ず、RPに戻って「ログインに失敗した」と表示される。`error: invalid_scope`、`Invalid scopes: openid expenses:approve`。
   申請アプリには`expenses:approve`を割り当てていない(Keycloakが許していない)ため、ユーザーが誰でも、ログイン自体が拒否される。
   [通信のトレース]の「2. Keycloakから戻る」にも、同じエラーが表示される。
3. `経費承認アプリ`に変えて、`expenses:write`にチェックを入れてログインする。→ 同じく`invalid_scope`。承認アプリには`expenses:write`を割り当てていない。
4. `経費申請アプリ`、`expenses:write`にチェックを入れて、`admin.sios`(部長)でログインする。→ ログインできる。
5. `GET 承認待ちの一覧`を押す。→ **200**。承認待ちの一覧は、`expenses:read`とロール`my-manager-role`で見られる。(スコープ`expenses:approve`は要らない)
6. `POST 経費を承認する`を、IDを`1`にして押す。→ **403**。ロール`my-admin-role`はあるが、スコープ`expenses:approve`が無い。
   部長でも、申請アプリでは承認を要求できない。
7. ログアウトし、`経費承認アプリ`、チェックは**どちらも外して**、`admin.sios`でログインする。
8. `POST 経費を承認する`を、IDを`1`にして押す。→ **403**。ロールはあるが、スコープ`expenses:approve`を要求していない。

### まとめ(期待される結果)

承認に関するAPI(承認待ちの一覧と、承認)は、スコープとロールの**両方**が必要。「このアプリに許したか(スコープ)」と「このユーザーに許されているか(ロール)」の両方を満たす必要がある。
ロールは、一般社員 < 課長 < 部長の3段階。承認待ちの一覧は、読むスコープ(`expenses:read`)と課長以上のロール、承認は、承認のスコープ(`expenses:approve`)と部長のロールが要る。

| アプリ | optionalスコープ | ユーザー         | 一覧 | 申請 | 承認待ち | 承認 | 拒否の理由                                       |
|--------|-------------------|------------------|------|------|----------|------|--------------------------------------------------|
| 申請   | なし              | staff.sios(一般社員) | 200  | 403  | 403      | 403  | 申請はwriteスコープ無し。承認待ちと承認はロール不足    |
| 申請   | あり(write)      | staff.sios(一般社員) | 200  | 201  | 403      | 403  | 承認待ちと承認はロール不足                         |
| 申請   | あり(write)      | admin.sios(部長)   | 200  | 201  | 200      | 403  | 承認はapproveスコープ無し(申請アプリには割り当てていない) |
| 申請   | あり(approve)    | 誰でも           | -    | -    | -        | -    | **ログインが拒否される**(invalid_scope)             |
| 承認   | あり(write)      | 誰でも           | -    | -    | -        | -    | **ログインが拒否される**(invalid_scope)             |
| 承認   | なし              | admin.sios(部長)   | 200  | 403  | 200      | 403  | 承認はapproveスコープ無し                          |
| 承認   | あり(approve)    | staff.sios(一般社員) | 200  | 403  | 403      | 403  | ロール不足(課長以上ではない)                      |
| 承認   | あり(approve)    | taro.sios(課長)    | 200  | 403  | 200      | 403  | 承認はロール不足(部長のみ)                        |
| 承認   | あり(approve)    | admin.sios(部長)   | 200  | 403  | 200      | 200  | -                                                |

(表の「申請」「承認」はアプリ。「申請」列は`POST /apis/expenses`、「承認」列は`POST /apis/expenses/{id}/approve`の結果)

### 興味のある人向け: 仕組みを読む

| 知りたいこと                                   | 見る場所                                                                            |
|------------------------------------------------|-------------------------------------------------------------------------------------|
| 認可リクエスト・トークン交換・PKCEの実装       | `webapp/relying-party/.../service/AuthorizationService.java`(ステップ順に並んでいる。入口と全体の流れは`controller/RelyingPartyController.java`) |
| 画面の表示(結果欄やトレースの部分更新を含む)     | `webapp/relying-party/src/main/resources/templates/index.html`、`fragments.html`    |
| トークンの検証、スコープ/ロールの権限への変換    | `webapp/resource-server/.../security/SecurityConfig.java`                           |
| APIごとの許可条件                              | `webapp/resource-server/.../controller/ExpenseController.java`の`@PreAuthorize`   |
| 経費データ(メモリ上)                          | `webapp/resource-server/.../service/ExpenseService.java`                            |
| 403のときに理由を返す処理                      | `webapp/resource-server/.../controller/ForbiddenAdvice.java`                        |
| 宛先(aud)の検証                                | `webapp/resource-server/src/main/resources/application.yaml`の`audiences`          |
| 条件の組み合わせごとのテスト                   | `webapp/resource-server/src/test/.../AccessControlTests.java`                       |
| Keycloak側の設定(スコープ、audience、クライアント、ロール、グループ)  | `setup/step02〜step10`                                                              |

試してみること:
- `ExpenseController`の`@PreAuthorize`を変更し(例: `my-admin-role`を`my-manager-role`に)、以下で反映して、結果の違いを見る。
    $ cd container && docker compose restart resource-server
- RSを直接呼ぶと、アクセストークンが無いので401になる。レスポンスの`WWW-Authenticate`も見る。
    $ curl -i http://localhost:8111/apis/whoami
- Keycloak管理画面で、`staff.sios`を課長のグループ`my-manager-grp`に所属させて(Users > staff.sios > Groups)、再ログインすると承認待ちの一覧が見られるようになる。
- Keycloak管理画面 > Realm roles > `my-admin-role` > [Associated roles] で、部長のロールが課長と一般社員のロールを含んでいること(複合ロール)を確認する。
- Keycloak管理画面 > Clients > app-expense-claim > [Client scopes] で、`expenses:write`を Optional から Default に変えると、チェックの有無にかかわらず付与される。
- Keycloak管理画面 > Clients > app-expense-claim > [Client scopes] で、`expenses:approve`を Optional として追加すると、申請アプリで`expenses:approve`を要求してもエラーにならず、付与される。(何を許すかはKeycloakの設定で決まる)
- `webapp/resource-server/src/main/resources/application.yaml`の`audiences`を別の値(例: `other-api`)に変えて、RSを再起動する。すべてのAPIが401になる。(宛先が違うトークンは受け付けない)

### トラブルシュート

- http://localhost:8222/ にアクセスできない
    -> まだビルド中の可能性がある。`docker logs relying-party`で`Started Application`が出るまで待つ。
       ビルドに失敗している場合は、同じログにエラーが出る。
- ログイン後に「IDトークンの検証」が`NG`になる(`iss`が一致しない)
    -> Keycloakが`KC_HOSTNAME`の設定なしで起動している。`./CREATE_CONTAINER.sh`でコンテナを作り直す。
- Keycloakで`Invalid parameter: redirect_uri`と表示される
    -> クライアントの`Valid redirect URIs`が`http://localhost:8222/*`でない。`setup/step04`で作成したクライアントを使う。
- ログイン後、APIがすべて401になる
    -> RSの`issuer-uri`と、トークンの`iss`が一致していない。(上記の`KC_HOSTNAME`の件)
- ソースを変更したのに反映されない
    -> `docker compose restart relying-party resource-server`で、ソースを取り込み直す。
