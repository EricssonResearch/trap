#!/bin/bash

set -e
unset CDPATH
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

IFS=. read major minor maint <<<"$2"

nmajor=$(($major+1))
nminor=$(($minor+1))
nmaint=$(($maint+1))

case $1 in 

	"major")
# Do this sometime
	;;

	"minor")
		$DIR/release.sh $major.$minor $major.$nminor
	;;

	"maint")
		$DIR/minor_release.sh $major.$minor.$maint $major.$minor.$nmaint
	;;

esac
