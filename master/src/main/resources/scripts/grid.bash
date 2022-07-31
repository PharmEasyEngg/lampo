#!/bin/bash

set +e


if [ -f ~/.bash_profile ]; then source ~/.bash_profile; fi
if [ -f  ~/.bashrc ]; then source  ~/.bashrc; fi
if [ -f  ~/.profile ]; then source  ~/.profile; fi
if [ -f ~/.zshrc ]; then source ~/.zshrc; fi


cmd="$1/bin/java -jar '$2' -role hub -port $3 &> $4 2>&1 < /dev/null &"
echo "executing cmd => $cmd"
eval $cmd

disown