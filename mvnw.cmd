@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
if "_%MAVEN_PROJECTBASEDIR:~-1%"=="_\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

if not "%JAVA_HOME%"=="" goto OkJHome
for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
if not "%JAVACMD%"=="" goto checkJCmd

if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\jbr\bin\java.exe" (
  set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\jbr"
  goto OkJHome
)

for /d %%D in ("%USERPROFILE%\.jdks\*") do (
  if exist "%%D\bin\java.exe" set "JAVA_HOME=%%D"
)
if not "%JAVA_HOME%"=="" goto OkJHome

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:checkJCmd
if not exist "%JAVACMD%" (
  echo The JAVA_HOME environment variable is not defined correctly, >&2
  echo this environment variable is needed to run this program. >&2
  exit /b 1
)

if "%MAVEN_USER_HOME%"=="" (
  set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
)

set "MVN_DIR=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-3.9.9"
set "MAVEN_HOME=%MVN_DIR%\apache-maven-3.9.9"

if not exist "%MAVEN_HOME%\bin\m2.conf" (
  if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\m2.conf" (
    set "MAVEN_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3"
  ) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference = 'Stop'; ^
       $props = Get-Content -Raw '%~dp0.mvn\wrapper\maven-wrapper.properties' | ConvertFrom-StringData; ^
       $url = $props.distributionUrl; ^
       $dest = '%MVN_DIR%'; ^
       New-Item -ItemType Directory -Force -Path $dest | Out-Null; ^
       $zip = Join-Path $dest 'maven.zip'; ^
       Invoke-WebRequest -Uri $url -OutFile $zip; ^
       Expand-Archive -Path $zip -DestinationPath $dest -Force; ^
       Remove-Item $zip -Force;"
  )
)

for %%i in ("%MAVEN_HOME%"\boot\plexus-classworlds-*) do set CLASSWORLDS_JAR="%%i"

set "MAVEN_CMD_LINE_ARGS=%*"
if defined MAVEN_CMD_LINE_ARGS (
  set "MAVEN_CMD_LINE_ARGS=%MAVEN_CMD_LINE_ARGS:-Dspring-boot .run.profiles=-Dspring-boot.run.profiles%"
)

"%JAVACMD%" ^
  --enable-native-access=ALL-UNNAMED ^
  %MAVEN_OPTS% ^
  -classpath %CLASSWORLDS_JAR% ^
  "-Dclassworlds.conf=%MAVEN_HOME%\bin\m2.conf" ^
  "-Dmaven.home=%MAVEN_HOME%" ^
  "-Dlibrary.jansi.path=%MAVEN_HOME%\lib\jansi-native" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.codehaus.plexus.classworlds.launcher.Launcher %MAVEN_CMD_LINE_ARGS%

exit /b %ERRORLEVEL%
