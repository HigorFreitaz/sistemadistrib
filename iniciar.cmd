@echo off
rem Liga o servidor e abre o cliente do SD Garagem, cada um na sua propria
rem janela. Basta dar dois cliques neste arquivo.
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\jbr"
start "SD Garagem - Servidor" /D "%~dp0server" cmd /k ..\mvnw.cmd -q javafx:run
start "SD Garagem - Cliente" /D "%~dp0client" cmd /k ..\mvnw.cmd -q javafx:run
