#!/bin/sh
# Compiles all sources into the out/ folder.
cd "$(dirname "$0")"
rm -rf out && mkdir out
javac -d out $(find src -name "*.java") && echo "Compiled OK. Now run ./run.sh"
