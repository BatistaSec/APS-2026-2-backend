@echo off
chcp 65001 > nul
cls
echo ===============================================================
echo    Sistema de Estacao de Monitoramento Ambiental (APS)
echo    Backend Java 21 + Hibernate + MySQL (Padrao DAO)
echo ===============================================================
echo.

echo [*] Compilando e iniciando com Maven...
call mvn compile exec:java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Ocorreu uma falha na compilacao ou execucao!
    echo Verifique se o MySQL esta rodando e as credenciais estao corretas em hibernate.cfg.xml.
    pause
    exit /b %ERRORLEVEL%
)

pause
