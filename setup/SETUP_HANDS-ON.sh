#!/bin/sh

clear
S_TIME=$(date +%s)
CUR_DIR=$(cd $(dirname $0); pwd)

echo "############################################################"
echo "# START SCRIPT"
echo "############################################################"

$CUR_DIR/step01-create_realm.sh
$CUR_DIR/step02-create_client_scope.sh
$CUR_DIR/step03-assign_audience_to_client_scope.sh
$CUR_DIR/step04-create_client.sh
$CUR_DIR/step05-assign_client_scope_to_client.sh
$CUR_DIR/step06-create_group.sh
$CUR_DIR/step07-create_realm_role.sh
$CUR_DIR/step08-assign_realm_role_to_group.sh
$CUR_DIR/step09-create_user.sh
$CUR_DIR/step10-assign_user_to_group.sh

E_TIME=$(date +%s)
DURATION=$((E_TIME - S_TIME))

echo "############################################################"
echo "# FINISH SCRIPT ($DURATION seconds)"
echo "############################################################"
