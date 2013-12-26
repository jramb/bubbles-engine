#!/usr/bin/env sh

#./bubbles buload -ul -f $1 -d ICHNAEA -b "http://localhost:8090"
./bubbles buload -ul -f $1 -d $BUBBLE_DOMAIN -b $BUBBLE_URL
