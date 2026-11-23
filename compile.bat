@echo off
rem Compiles all sources into the out folder.
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
if %errorlevel%==0 echo Compiled OK. Now run run.bat
