#!/bin/bash

DIST_DIR=`dirname $0`
$DIST_DIR/jasypt/bin/encrypt.sh "$@"