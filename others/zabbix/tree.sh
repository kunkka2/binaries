#!/bin/sh
set -e
#options_nix.go
echo " run ls "
ls 
echo " run ls 7.4.11"
ls 7.4.11
echo "get options_nix.go and mv"
curl -L https://github.com/kunkka2/binaries/raw/refs/heads/main/others/zabbix/options_nix.go -o 7.4.11/src/go/internal/agent/options_nix.go
#serverconnector.go  zabbix-7.2.6/src/go/internal/agent/serverconnector/serverconnector.go
echo "serverconnector.go"
curl -L https://github.com/kunkka2/binaries/raw/refs/heads/main/others/zabbix/serverconnector.go -o 7.4.11/src/go/internal/agent/serverconnector/serverconnector.go


#comms.go zabbix-7.2.6/src/go/pkg/zbxcomms/comms.go
echo "zbxcomms/comms.go"
curl -L https://github.com/kunkka2/binaries/raw/refs/heads/main/others/zabbix/comms.go -o zabbix-7.4.11/src/go/pkg/zbxcomms/comms.go