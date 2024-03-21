#!/usr/bin/env just --justfile
set dotenv-load
set dotenv-filename := ".env.local"
default:
  just --list


dota2:
  @nu -c 'echo $"run opendota.nu with NOREQ=($env.NOREQ) on CMD_7Z=($env.CMD_7Z)"'
  @nu ./nushell/opendota.nu