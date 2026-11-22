#!/bin/sh
cd "$(dirname "$0")"
exec java -Xmx256m -cp "build:lib/*" it.unibo.ai.didattica.competition.tablut.alielegio.OurClient "$1" "$2" "$3"
