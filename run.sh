#!/bin/sh
# Runs the program. Any jar in lib/ (e.g. mysql-connector-j) is added to the classpath.
# Usage: ./run.sh [--demo] [--memory]
cd "$(dirname "$0")"
java -cp "out:lib/*" ticketing.Main "$@"
