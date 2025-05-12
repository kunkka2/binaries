#!/bin/sh
set -e
#options_nix.go
curl -L https://github.com/kunkka2/binaries/raw/refs/heads/main/others/zabbix/options_nix.go -o zabbix-7.2.6/src/go/internal/agent/options_nix.go
#serverconnector.go  zabbix-7.2.6/src/go/internal/agent/serverconnector/serverconnector.go
curl -L https://github.com/kunkka2/binaries/raw/refs/heads/main/others/zabbix/serverconnector.go -o zabbix-7.2.6/src/go/internal/agent/serverconnector/serverconnector.go


#comms.go zabbix-7.2.6/src/go/pkg/zbxcomms/comms.go
curl -L https://github.com/kunkka2/binaries/raw/refs/heads/main/others/zabbix/comms.go -o zabbix-7.2.6/src/go/pkg/zbxcomms/comms.go