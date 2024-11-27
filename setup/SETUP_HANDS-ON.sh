#!/bin/sh

clear
S_TIME=$(date +%s)
CUR_DIR=$(cd $(dirname $0); pwd)

echo "############################################################"
echo "# START SCRIPT"
echo "############################################################"

$CUR_DIR/step01-create_realm.sh
$CUR_DIR/step02-create_client_scope.sh
$CUR_DIR/step03-create_client.sh
$CUR_DIR/step04-map_audience_to_client_scope.sh

##########
##########
##########

PATH_BASE_DIR="$(dirname $(pwd)/$0)/"
. $PATH_BASE_DIR/call_kc_apis.sh
. $PATH_BASE_DIR/comb_kc_apis.sh

HST="http://localhost:8080"
ADM_USER="admin"
ADM_PASS="admin"

MY_REALM="handson"

echo "##### START ######################################"

	AC_TOKEN=$(call_fetch_master_access_token $HST $ADM_USER $ADM_PASS)

#	call_create_realm $HST $AC_TOKEN $MY_REALM

#	call_create_client_scope $HST $AC_TOKEN $MY_REALM "read:my-scope"
#	call_create_client_scope $HST $AC_TOKEN $MY_REALM "write:my-scope"
#	call_create_client_scope $HST $AC_TOKEN $MY_REALM "read:baseball.manager"
#	call_create_client_scope $HST $AC_TOKEN $MY_REALM "read:baseball.player"
#	call_create_client_scope $HST $AC_TOKEN $MY_REALM "read:soccer.manager"
#	call_create_client_scope $HST $AC_TOKEN $MY_REALM "read:soccer.player"
	JSON_SCP=$(call_get_client_scopes $HST $AC_TOKEN $MY_REALM)

#	call_create_client $HST $AC_TOKEN $MY_REALM "my-web-app" "http://localhost:8222/*,http://localhost:8333/*" false
#	call_create_client $HST $AC_TOKEN $MY_REALM "my-api-gateway" "" false
#	call_create_client $HST $AC_TOKEN $MY_REALM "app-baseball" "http://localhost:8222/*,http://localhost:8333/*" true
#	call_create_client $HST $AC_TOKEN $MY_REALM "app-soccer" "http://localhost:8222/*,http://localhost:8333/*" true
	JSON_CLT=$(call_get_clients $HST $AC_TOKEN $MY_REALM)

#	run_map_client_scope_audience $HST $AC_TOKEN $MY_REALM "$JSON_SCP" "read:my-scope" "my-web-app"
#	run_map_client_scope_audience $HST $AC_TOKEN $MY_REALM "$JSON_SCP" "write:my-scope" "my-web-app"
#	run_map_client_scope_audience $HST $AC_TOKEN $MY_REALM "$JSON_SCP" "read:baseball.manager" "app-baseball"
#	run_map_client_scope_audience $HST $AC_TOKEN $MY_REALM "$JSON_SCP" "read:baseball.player" "app-baseball"
#	run_map_client_scope_audience $HST $AC_TOKEN $MY_REALM "$JSON_SCP" "read:soccer.manager" "app-soccer"
#	run_map_client_scope_audience $HST $AC_TOKEN $MY_REALM "$JSON_SCP" "read:soccer.player" "app-soccer"

#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:my-scope"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "write:my-scope"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:baseball.manager"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:baseball.player"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:soccer.manager"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:soccer.player"

#	run_assing_opt_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-baseball" "read:baseball.manager"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-baseball" "read:baseball.player"
#	run_assing_opt_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-soccer" "read:soccer.manager"
#	run_assing_def_client_scope_to_client \
#		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-soccer" "read:soccer.player"

#	call_create_group $HST $AC_TOKEN $MY_REALM "my-admin-grp"
#	call_create_group $HST $AC_TOKEN $MY_REALM "my-staff-grp"
#	call_create_group $HST $AC_TOKEN $MY_REALM "my-flat-grp"
	JSON_GRP=$(call_get_groups $HST $AC_TOKEN $MY_REALM)

	call_create_realm_role $HST $AC_TOKEN $MY_REALM "my-admin-role"
	call_create_realm_role $HST $AC_TOKEN $MY_REALM "my-staff-role"
	JSON_ROL=$(call_get_realm_roles $HST $AC_TOKEN $MY_REALM)

	run_assing_realm_role_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_ROL" "my-admin-grp" "my-admin-role"
	run_assing_realm_role_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_ROL" "my-staff-grp" "my-staff-role"
	run_assing_realm_role_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_ROL" "my-flat-grp" "my-admin-role"
	run_assing_realm_role_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_ROL" "my-flat-grp" "my-staff-role"

##	call_enter_message "password of user to create"
##	PASSWD=$(call_enter_capture)
	USER_NM="admin.sios"
	call_create_user $HST $AC_TOKEN $MY_REALM $USER_NM $USER_NM
	USER_NM="staff.sios"
	call_create_user $HST $AC_TOKEN $MY_REALM $USER_NM $USER_NM
	USER_NM="taro.sios"
	call_create_user $HST $AC_TOKEN $MY_REALM $USER_NM $USER_NM
	JSON_USR=$(call_get_users $HST $AC_TOKEN $MY_REALM)

	run_assing_user_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-admin-grp" "admin.sios"
	run_assing_user_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-staff-grp" "staff.sios"
	run_assing_user_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-flat-grp" "taro.sios"


echo "\n##### FINISH #####################################"

##########
##########
##########

E_TIME=$(date +%s)
DURATION=$((E_TIME - S_TIME))

echo "############################################################"
echo "# FINISH SCRIPT ($DURATION seconds)"
echo "############################################################"
