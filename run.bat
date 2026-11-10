@echo off
setlocal
cd /d "%~dp0"
if not exist out\checkout\Main.class call "%~dp0build.bat" || exit /b 1
java -cp "out;web" checkout.Main
