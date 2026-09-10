@echo off
rem Liga o servidor e abre o cliente do SD Garagem numa unica janela de
rem console. Basta dar dois cliques neste arquivo.
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "ROOT=%~dp0"
echo Iniciando servidor em segundo plano...
start /B cmd /c "cd /d "%ROOT%server" && "%ROOT%mvnw.cmd" -q javafx:run"
echo Abrindo cliente...
cd /d "%ROOT%client"
call "%ROOT%mvnw.cmd" -q javafx:run
