#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

set_java_home() {
  if [ -n "${JAVA_HOME-}" ]; then
    if [ -x "$JAVA_HOME/bin/java" ]; then
      JAVACMD="$JAVA_HOME/bin/java"
    else
      echo "The JAVA_HOME environment variable is not defined correctly, so Apache Maven cannot be started." >&2
      echo "JAVA_HOME is set to \"$JAVA_HOME\", capability \"\$JAVA_HOME/bin/java\" does not exist." >&2
      exit 1
    fi
  else
    JAVACMD="$(
      'unset' -f command 2>/dev/null
      'unset' -v command 2>/dev/null
      command -v java
    )" || :
    if [ ! -x "${JAVACMD-}" ]; then
      echo "The java command could not be found in your PATH, nor is JAVA_HOME set." >&2
      exit 1
    fi
  fi
}

hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(((h * 31 + $(LC_CTYPE=C printf %d "'$char")) % 4294967296))
    str="${str#?}"
  done
  printf %x\\n $h
}

basedir=${0%/*}
case $basedir in
*/*) cd -- "$basedir" || exit 1 ;;
esac
basedir=$(pwd -P)

wrapper_Props="$basedir/.mvn/wrapper/maven-wrapper.properties"
distributionUrl=""
while IFS="=" read -r key value; do
  case "$key" in
  distributionUrl) distributionUrl=$(printf "%s" "$value" | tr -d '\r') ;;
  esac
done <"$wrapper_Props"

if [ -z "$distributionUrl" ]; then
  echo "cannot read distributionUrl property in $wrapper_Props" >&2
  exit 1
fi

distributionUrlName="${distributionUrl##*/}"
distributionUrlNameMain="${distributionUrlName%.*}"
distributionUrlNameMain="${distributionUrlNameMain%-bin}"
MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MAVEN_HOME="${MAVEN_USER_HOME}/wrapper/dists/${distributionUrlNameMain}/$(hash_string "$distributionUrl")"

exec_maven() {
  unset MVNW_VERBOSE MVNW_USERNAME MVNW_PASSWORD MVNW_REPOURL || :
  exec "$MAVEN_HOME/bin/mvn" "$@" || exit 1
}

if [ -x "$MAVEN_HOME/bin/mvn" ]; then
  exec_maven "$@"
fi

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

mkdir -p "$MAVEN_HOME"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
if command -v curl >/dev/null 2>&1; then
  curl -fL "$distributionUrl" -o "$tmp_dir/$distributionUrlName"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$tmp_dir/$distributionUrlName" "$distributionUrl"
else
  echo "Cannot download Maven wrapper distribution: neither curl nor wget is installed." >&2
  exit 1
fi
unzip -q "$tmp_dir/$distributionUrlName" -d "$tmp_dir"
mv "$tmp_dir/$distributionUrlNameMain"/* "$MAVEN_HOME"/
exec_maven "$@"

