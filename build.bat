@echo off
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
if exist sources.txt del sources.txt
for /r "src\main" %%f in (*.java) do echo "%%f">>sources.txt
powershell -NoProfile -Command "(Get-Content sources.txt).Replace([char]92,[char]47) | Set-Content -Encoding ascii sources.txt"
javac --release 17 -encoding UTF-8 -d out @sources.txt
set ERR=%ERRORLEVEL%
del sources.txt
if %ERR% neq 0 (echo Build GAGAL & exit /b %ERR%)
echo Build selesai -^> out\
