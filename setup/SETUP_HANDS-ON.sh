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




E_TIME=$(date +%s)
DURATION=$((E_TIME - S_TIME))

echo "############################################################"
echo "# FINISH SCRIPT ($DURATION seconds)"
echo "############################################################"
