#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Verwendung: $0 <datei.csv>" >&2
    exit 1
fi

INPUT_FILE="$1"

if [[ ! -f "$INPUT_FILE" ]]; then
    echo "Fehler: Datei '$INPUT_FILE' existiert nicht." >&2
    exit 1
fi

if [[ ! -r "$INPUT_FILE" ]]; then
    echo "Fehler: Datei '$INPUT_FILE' ist nicht lesbar." >&2
    exit 1
fi

awk -F';' '
    NR==1 { next }
    NF>0 && $1 && $3 && ($1 !~ /\s/)  && ($1 !~ /"/) && ($0 ~ /[A-Za-z]/) {   # appname ohne Leerzeichen, mindestens ein Buchstabe
        gsub(/\r/,"")
        printf "%s\r\n", $1 "," $3
    }
' "$INPUT_FILE"
