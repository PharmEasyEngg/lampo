#!/bin/bash

set +e

source ~/.bash_profile
source ~/.bashrc
source ~/.profile
source ~/.zshrc

node_version="14.5.0"

java_home="$1"
android_home="$2"
appium_cmd="${@:3}"

cmd="nvm use --delete-prefix v$node_version --silent &>/dev/null && export ANDROID_HOME=\"$android_home\" && export JAVA_HOME=\"$java_home\" && appium $appium_cmd"
echo "executing cmd => $cmd"
eval $cmd

disown