@echo off
rem Abre so o cliente do SD Garagem (sem subir servidor junto). Rode este
rem arquivo mais de uma vez -- um clique por cliente -- para testar varios
rem clientes conectados ao mesmo tempo no mesmo servidor.
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "ROOT=%~dp0"
cd /d "%ROOT%client"
call "%ROOT%mvnw.cmd" -q javafx:run
