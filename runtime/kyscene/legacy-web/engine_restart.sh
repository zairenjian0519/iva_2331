#!/bin/sh
set -eu

/usr/bin/sudo -n /usr/bin/systemctl restart ai-engine-config-restart.timer
/usr/bin/systemctl is-active --quiet ai-engine-config-restart.timer
