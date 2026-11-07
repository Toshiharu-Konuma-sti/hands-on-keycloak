#!/bin/sh
set -e

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. ${CUR_DIR}/.env-keycloak

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token ${KC_URL} ${KEYCLOAK_ADMIN} ${KEYCLOAK_ADMIN_PASSWORD})

echo "\n### START: Create a realm ##########"
call_create_realm $KC_URL $AC_TOKEN $MY_REALM

# 同意画面に表示する、アプリ名とスコープ名の日英の文言。
# step02(スコープ)とstep04(クライアント)から、このキーを ${キー} の形式で参照する。
echo "\n### START: Register message texts for the consent screen ##########"
call_import_localization $KC_URL $AC_TOKEN $MY_REALM "ja" '{
	"client_app_expense_claim": "経費申請アプリ (app-expense-claim)",
	"client_app_expense_approval": "経費承認アプリ (app-expense-approval)",
	"scope_expenses_read": "経費の参照 (expenses:read)",
	"scope_expenses_write": "経費の申請 (expenses:write)",
	"scope_expenses_approve": "経費の承認 (expenses:approve)"
}'
call_import_localization $KC_URL $AC_TOKEN $MY_REALM "en" '{
	"client_app_expense_claim": "Expense Claim App (app-expense-claim)",
	"client_app_expense_approval": "Expense Approval App (app-expense-approval)",
	"scope_expenses_read": "View expenses (expenses:read)",
	"scope_expenses_write": "Submit expenses (expenses:write)",
	"scope_expenses_approve": "Approve expenses (expenses:approve)"
}'

call_show_finish_banner
