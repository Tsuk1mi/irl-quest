@echo off
REM filepath: scripts\ollama_infer.bat
REM Usage: ollama_infer.bat "<input>" "<model>"
setlocal enabledelayedexpansion
if "%~2"=="" (
  echo Usage: %~nx0 "<input>" "<model>"
  exit /b 1
)
set INPUT=%~1
set MODEL=%~2

REM If PowerShell wrapper exists, use it for safer quoting/JSON handling
if exist "%~dp0ollama_infer.ps1" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0ollama_infer.ps1" -Model "%MODEL%" -Input "%INPUT%"
) else (
  ollama run %MODEL% "%INPUT%"
)
