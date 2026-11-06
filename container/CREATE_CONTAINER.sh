#!/bin/sh

S_TIME=$(date +%s)
CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/common.sh
. $CUR_DIR/custom.sh

case "$1" in
	"up")
		clear
		start_banner
		check_required_commands "docker"
		create_container $CUR_DIR
		finish_banner $S_TIME
		;;
	"down")
		clear
		start_banner
		check_required_commands "docker"
		destory_container $CUR_DIR
		show_list_container
		finish_banner $S_TIME
		;;
	"rebuild")
		clear
		start_banner
		check_required_commands "docker"
		rebuild_container $CUR_DIR $2
		show_list_container
		finish_banner $S_TIME
		;;
	"list")
		clear
		check_required_commands "docker"
		show_list_container
		;;
	"info")
		show_url
		;;
	"")
		clear
		start_banner
		check_required_commands "docker"
		destory_container $CUR_DIR
		create_container $CUR_DIR

		show_list_container
		show_url
		finish_banner $S_TIME
		;;
	*)
		show_usage
		exit 1
		;;

esac

