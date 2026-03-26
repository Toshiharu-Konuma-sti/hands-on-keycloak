
# {{{ start_banner()
start_banner()
{
	echo "############################################################"
	echo "# START SCRIPT"
	echo "############################################################"
}
# }}}

# {{{ finish_banner()
# $1: time to start this script
finish_banner()
{
	S_TIME=$1
	E_TIME=$(date +%s)
	DURATION=$((E_TIME - S_TIME))
	echo "############################################################"
	echo "# FINISH SCRIPT ($DURATION seconds)"
	echo "############################################################"
}
# }}}


# {{{ function check_required_commands()
check_required_commands()
{
	echo "\n### START: Check required commands ##########"
	local missing_cmds=""
	for cmd in $*; do
		if ! command -v "${cmd}" >/dev/null 2>&1; then
			if [ -z "$missing_cmds" ]; then
				missing_cmds="${cmd}"
			else
				missing_cmds="${missing_cmds} ${cmd}"
			fi
		fi
	done
	if [ -n "$missing_cmds" ]; then
		echo "========================================================" >&2
		echo " [ERROR] The following required commands are missing." >&2
		echo " Please install them or check your PATH to continue." >&2
		echo "========================================================" >&2
		for missing in $missing_cmds; do
			echo " - ${missing}" >&2
		done
		echo "" >&2
		exit 1
	fi
}
# }}}

# {{{ install_jdk()
install_jdk()
{
	echo "\n### START: Install Open JDK ##########"
	which java
	if [ $? -ne 0 ]; then
		sudo apt install -y openjdk-21-jdk-headless
	fi
}
# }}}


# {{{ show_list_container()
show_list_container()
{
	echo "\n### START: Show a list of container ##########"
	docker ps -a
}
# }}}

