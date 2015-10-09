#!/bin/bash

((!$#)) && echo Usage: minor_release.sh rel_minor next_minor && exit 1
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
# etc/scripts/release
cd ../../..
mvn versions:set -DnewVersion=$1
mvn -Prelease clean install -DskipTests
mvn -Prelease deploy -DskipTests
git commit -a -m "$1 Release"
git tag -a v$1 -m "Release v$1"
mvn versions:set -DnewVersion="$2-SNAPSHOT"
git commit -a -m "$2 Snapshot"
