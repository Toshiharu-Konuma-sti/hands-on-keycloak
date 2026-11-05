#!/bin/sh
# アプリストアへ接続できない環境でもコンテナ起動を止めない
php occ app:install user_saml || echo "WARN: failed to install user_saml. Install it manually from the Apps page."

# user_samlはsaml_dataをSameSite=Noneで発行するが、HTTPではSecure属性が付かずChromeが破棄してログインがループする。
# localhost同士はsame-siteなのでLaxで足りる。
sed -i "s/\(saml_data', \$data, null, '\)None'/\1Lax'/" custom_apps/user_saml/lib/Controller/SAMLController.php
