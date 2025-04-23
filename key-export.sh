#!/usr/bin/env bash

set -e

splitLine() {
  echo "########################################################"
}

echo "GPG key Exporter v0.0.1"

splitLine
gpg --list-keys --keyid-format LONG
splitLine

echo "Listed your keys. Please enter the key want to export"
read -r -p '> ' keyId

keySecret=$(gpg --armor --export-secret-key "$keyId")

# shellcheck disable=SC2162
read -s -p 'Key password (Enter directly if no password): ' keyPassword
echo

printQuoted() {
  echo -n '"'
  echo -n "$1" | sed -e 's/\\/\\\\/g' | sed ':a;N;$!ba;s/\n/\\n/g'
  echo -n '"'
}

generateComputed() {
  echo -n '{'
  printQuoted 'key'
  echo -n ':'
  printQuoted "$keySecret"

  if [ -n "$keyPassword" ]; then
    echo -n ','
    printQuoted 'keyPassword'
    echo -n ':'
    printQuoted "$keyPassword"
  fi
  echo -n '}'
}

echo "Type save location, leave empty to use stdout"
read -r -p '> ' outputFile

if [[ "$outputFile" == '-' || "$outputFile" == "" ]]; then
  generateComputed
else
  generateComputed > "$outputFile"
fi

                    