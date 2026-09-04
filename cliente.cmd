@echo off
setlocal
rem Abre o cliente SD Garagem. Basta dar dois cliques neste arquivo.
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\jbr"
cd /d "%~dp0client"
call ..\mvnw.cmd -q javafx:run
endlocal
pause
