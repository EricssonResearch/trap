#!/bin/bash
((!$#)) && echo "Usage: release.sh rel_major next_major (eg release.sh 1.2 1.3) " && exit 1
set -e
unset CDPATH
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
cd $DIR
cd ..
git checkout -b maint-$1
cd trap-parent
mvn -Pcc versions:set -DnewVersion=$1
mvn -Pcc clean install
mvn -Pcc deploy
cd ..
git commit -a -m "$1 Release"
git tag -a v$1 -m "Release v$1"
cd trap-parent
mvn -Pcc versions:set -DnewVersion=$1.1-SNAPSHOT
cd ..
git commit -a -m "$1.1 Snapshot"
git checkout master
cd trap-parent
mvn -Pcc versions:set -DnewVersion $2.0-SNAPSHOT
cd ..
git commit -a -m "$2.0 Snapshot"
