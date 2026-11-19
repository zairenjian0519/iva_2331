#!/usr/bin/env bash
set -Eeuo pipefail

BACKUP_DIR=${1:?usage: rollback_sync.sh BACKUP_DIR}
PROD=/userdata/sps_test/iva_python/duchun_rk182x_python
[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
case "$BACKUP_DIR" in /userdata/sps_test/iva_python/rk182x-sync-backups/*) ;; *) echo "invalid backup path" >&2; exit 1 ;; esac
[ -f "$BACKUP_DIR/project.tar.gz" ]

systemctl stop ai_engine.service || true
tar -C "$PROD" -xzf "$BACKUP_DIR/project.tar.gz"
rm -rf /etc/systemd/system/ai_engine.service.d
[ ! -f "$BACKUP_DIR/engine-systemd.tar.gz" ] || tar -C /etc/systemd/system -xzf "$BACKUP_DIR/engine-systemd.tar.gz"
install -m 0644 "$BACKUP_DIR/ai_engine.service" /etc/systemd/system/ai_engine.service
if [ -f "$BACKUP_DIR/duchun-rk182x-python.env" ]; then
    install -m 0644 "$BACKUP_DIR/duchun-rk182x-python.env" /etc/duchun-rk182x-python.env
fi
systemctl daemon-reload
systemctl enable ai_engine.service >/dev/null
systemctl restart ai_engine.service
systemctl is-active --quiet ai_engine.service
echo "rollback completed: RK3588 engine restored"
