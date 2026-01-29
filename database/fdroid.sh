#!/usr/bin/env bash

set -e

URL="https://f-droid.org/repo/index-v1.jar"
WORKDIR="app/src/main/res/raw"
OUTCSV="apps_categories.csv"

echo "Workdir: $WORKDIR"
cd "$WORKDIR"

echo "Load F-Droid Index..."
curl -L -o index-v1.jar "$URL"

echo "Unzip JSON Index..."
unzip -q index-v1.jar index-v1.json

echo "Create CSV: $OUTCSV"
echo "app_id,categories" > "$OUTCSV"

jq -r '
  .apps[]
  | select(.packageName != null)
  | .packageName as $id
  | (.categories // [])
  | join("|") as $cats
  | "\($id),\($cats)"
' index-v1.json >> "$OUTCSV"

rm index-v1.jar index-v1.json

echo "Done:"
echo "$WORKDIR/$OUTCSV"

