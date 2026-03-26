#!/bin/sh

# {{{ show_usage_webui()
show_usage_webui()
{
	cat << EOS
Usage: $(basename $0) [options]

Runs this project as a Spring Boot application. Also simplify execution
of common Gradle tasks.

Options:
  assemble              Assemble the outputs of this project.
  doc                   Generate the Javadoc.
  test                  Run the unit tests.

EOS
}
# }}}

S_TIME=$(date +%s)
CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/common.sh
#. $CUR_DIR/custom.sh

case "$1" in
	"assemble")
		start_banner
#		./gradlew assemble -x cyclonedxBom --info
		./gradlew assemble --info
		finish_banner $S_TIME
		;;
	"doc")
		start_banner
#		./gradlew javadoc -x cyclonedxBom
		./gradlew javadoc
		command -v tree >/dev/null 2>&1 && tree -L 2 build/docs/
		finish_banner $S_TIME
		;;
	"test")
		start_banner
#		./gradlew test -x cyclonedxBom
		./gradlew test
		finish_banner $S_TIME
		;;
	"")
		start_banner
#		install_jdk

		printf "Enter IdP's client_secret: "
		stty -echo
		read OPAQUE_CLIENT_SECRET
		stty echo
		printf "\n"
		export OPAQUE_CLIENT_SECRET

		echo "Test URL:"
		echo "- http://localhost:8091/"

		./gradlew clean
		./gradlew bootRun
		;;
	*)
		show_usage_webui
		;;
esac
