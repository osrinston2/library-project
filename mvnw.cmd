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
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop'; ^
   $props = Get-Content -Raw '%~dp0.mvn\wrapper\maven-wrapper.properties' | ConvertFrom-StringData; ^
   $url = $props.distributionUrl; ^
   $name = [System.IO.Path]::GetFileNameWithoutExtension($url) -replace '-bin$',''; ^
   $m2 = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $env:USERPROFILE '.m2' }; ^
   $dest = Join-Path $m2 \"wrapper\dists\$name\"; ^
   $mvn = Join-Path $dest \"$name\bin\mvn.cmd\"; ^
   if (-not (Test-Path $mvn)) { ^
     New-Item -ItemType Directory -Force -Path $dest | Out-Null; ^
     $zip = Join-Path $dest 'maven.zip'; ^
     Invoke-WebRequest -Uri $url -OutFile $zip; ^
     Expand-Archive -Path $zip -DestinationPath $dest -Force; ^
     Remove-Item $zip -Force; ^
   } ^
   & $mvn %*"
exit /b %ERRORLEVEL%

