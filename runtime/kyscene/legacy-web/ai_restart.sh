#!/bin/sh
set -eu

/usr/bin/sudo /usr/bin/systemctl restart ai_server.service
/usr/bin/sudo /usr/bin/systemctl restart ai_engine.service
/usr/bin/systemctl is-active --quiet ai_server.service
/usr/bin/systemctl is-active --quiet ai_engine.service
