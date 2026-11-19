#!/usr/bin/env bash
set -Eeuo pipefail

monitor=${1:?usage: test_monitor_signal.sh MONITOR_SCRIPT}
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT

(
    CLEANING=0
    log() { :; }
    deactivate() { touch "$work/cleaned"; }
    source <(sed -n '/^on_exit() {/,/^}/p; /^on_signal() {/,/^}/p' "$monitor")
    trap on_exit EXIT
    trap on_signal TERM
    kill -TERM "$BASHPID"
    touch "$work/continued"
)

[ -f "$work/cleaned" ]
[ ! -e "$work/continued" ]
