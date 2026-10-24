#!/bin/bash
cd "$(dirname "$0")"
exec java -cp "build:lib/*" \
  it.unibo.ai.didattica.competition.tablut.alielegio.OurClient "$1" "$2" "$3"
