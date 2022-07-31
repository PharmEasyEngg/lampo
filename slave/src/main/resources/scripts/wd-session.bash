#!/bin/bash

set +e

if [ -f ~/.bash_profile ]; then source ~/.bash_profile; fi
if [ -f  ~/.bashrc ]; then source  ~/.bashrc; fi
if [ -f  ~/.profile ]; then source  ~/.profile; fi
if [ -f ~/.zshrc ]; then source ~/.zshrc; fi

node_version="14.5.0"

java_home="$1"
android_home="$2"
appium_cmd="${@:3}"

cmd="nvm use --delete-prefix v$node_version --silent &>/dev/null && export ANDROID_HOME=\"$android_home\" && export JAVA_HOME=\"$java_home\" && appium $appium_cmd"
echo "executing cmd => $cmd"
eval $cmd

disown