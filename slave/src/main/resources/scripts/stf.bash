#!/bin/bash

set +e


if [ -f ~/.bash_profile ]; then source ~/.bash_profile; fi
if [ -f  ~/.bashrc ]; then source  ~/.bashrc; fi
if [ -f  ~/.profile ]; then source  ~/.profile; fi
if [ -f ~/.zshrc ]; then source ~/.zshrc; fi

node_version="8.16.1"

ip=$1
quality=$2

cmd="nvm use --delete-prefix v$node_version --silent &>/dev/null && export SCREEN_JPEG_QUALITY=$quality && stf local --public-ip $ip &> stf.log 2>&1 < /dev/null &"
echo "executing cmd => $cmd"
eval $cmd

disown