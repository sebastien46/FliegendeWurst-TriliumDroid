#!/usr/bin/env bash

export HOME=$(mktemp -d)
trilium-server & disown
sleep 10
curl 'http://localhost:8080/api/setup/new-document' -X POST
curl 'http://localhost:8080/set-password' -X POST --data-raw 'password1=1234&password2=1234'
