#!/bin/bash

set +e

source ~/.bash_profile
source ~/.bashrc
source ~/.profile
source ~/.zshrc

node_version="8.16.1"

ip=$1
quality=$2

cmd="nvm use --delete-prefix v$node_version --silent &>/dev/null && export SCREEN_JPEG_QUALITY=$quality && stf local --public-ip $ip &> stf.log 2>&1 < /dev/null &"
echo "executing cmd => $cmd"
eval $cmd

disown