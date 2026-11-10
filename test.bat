@echo off
setlocal
cd /d "%~dp0"
call "%~dp0build.bat" || exit /b 1
if exist out-test rmdir /s /q out-test
mkdir out-test
if exist tsources.txt del tsources.txt
for /r "src\test" %%f in (*.java) do echo "%%f">>tsources.txt
powershell -NoProfile -Command "(Get-Content tsources.txt).Replace([char]92,[char]47) | Set-Content -Encoding ascii tsources.txt"
javac --release 17 -encoding UTF-8 -cp out -d out-test @tsources.txt
set ERR=%ERRORLEVEL%
del tsources.txt
if %ERR% neq 0 (rmdir /s /q out-test & exit /b %ERR%)
java -cp "out;out-test;web" checkout.SelfTest
set ERR=%ERRORLEVEL%
rmdir /s /q out-test
exit /b %ERR%
