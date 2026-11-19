#!/bin/bash

LOG_FILE="/var/log/get_sn.log"
# 如权限不够，可改为：LOG_FILE="$HOME/get_sn.log"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"
}

get_sn_n150_n180() {
    if [[ -x "/usr/local/lynxi/bsp/tools/lyn_cmd" ]]; then
        sn=$(/usr/local/lynxi/bsp/tools/lyn_cmd misc sn_get 2>/dev/null | grep -oP '(?<=sn=).*')
        if [[ -n "$sn" ]]; then
            log "SN detected from N150/N180 (lyn_cmd) method: $sn"
            echo "$sn"
        fi
    fi
}

get_sn_n2110() {
    if command -v mcutools >/dev/null 2>&1; then
        sn=$(mcutools -s 2>/dev/null | grep -oP '(?<=SN: ).*')
        if [[ -n "$sn" ]]; then
            log "SN detected from N2110 (mcutools) method: $sn"
            echo "$sn"
        fi
    fi
}

get_sn_n200() {
    if command -v setinfo >/dev/null 2>&1; then
        sn=$(sudo setinfo show | grep "Serial Number" | awk -F ':' '{print $2}' | tr -d ' ')
        if [[ -n "$sn" ]]; then
            log "SN detected from N200 (setinfo) method: $sn"
            echo "$sn"
        fi
    fi
}

generate_fallback_sn() {
    fallback="EMBL$(date +%Y%m%d%H%M%S)"
    log "SN not found, using fallback: $fallback"
    echo "$fallback"
}

# 主流程
sn=$(get_sn_n150_n180)

if [[ -z "$sn" ]]; then
    sn=$(get_sn_n2110)
fi

if [[ -z "$sn" ]]; then
    sn=$(get_sn_n200)
fi

if [[ -z "$sn" ]]; then
    sn=$(generate_fallback_sn)
else
    log "Final SN used: $sn"
fi

# 输出最终 SN
echo "$sn"