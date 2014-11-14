#!/bin/sh
JSDOC=$1
$JSDOC -d target/out -c src/main/javascript/conf.json -u src/main/javascript/tutorials src/main/javascript/api src/main/javascript/api/README.md
