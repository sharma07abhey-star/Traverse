@echo off
rem Runs the program. Any jar in the lib folder (e.g. mysql-connector-j) is added to the classpath.
rem Usage: run.bat [--demo] [--memory]
cd /d "%~dp0"
java -cp "out;lib/*" ticketing.Main %*
