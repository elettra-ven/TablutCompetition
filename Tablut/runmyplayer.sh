#!/bin/sh
cd "$(dirname "$0")"
# The TT uses about 22 MiB; 256 MiB leaves headroom for the client and JVM.
exec java -Xmx256m -cp "build:lib/*" it.unibo.ai.didattica.competition.tablut.alielegio.OurClient "$1" "$2" "$3"
