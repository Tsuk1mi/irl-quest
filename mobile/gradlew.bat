@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%\

set DEFAULT_JVM_OPTS=

rem Поиск java.exe
set JAVA_EXE=
if defined JAVA_HOME set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if not defined JAVA_EXE set JAVA_EXE=java.exe

rem Проверка наличия gradle-wrapper.jar
if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
    echo Gradle wrapper JAR не найден. Запустите 'gradle wrapper' для генерации.
    exit /b 1
)

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

set WRAPPER_MAIN=org.gradle.wrapper.GradleWrapperMain

set CMD_LINE_ARGS=
:parseArgs
if "%1"=="" goto execute
    set CMD_LINE_ARGS=%CMD_LINE_ARGS% "%1"
    shift
    goto parseArgs

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" %WRAPPER_MAIN% %CMD_LINE_ARGS%
endlocal
