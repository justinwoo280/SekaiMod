#!/bin/bash
set -e

if [ -f ./env_java.sh ]; then
  source ./env_java.sh
fi
source ../buildScript/init/env_ndk.sh

BUILD=".build"

rm -rf $BUILD/android \
  $BUILD/java \
  $BUILD/javac-output \
  $BUILD/src

if [ -z "$GOPATH" ]; then
  GOPATH=$(go env GOPATH)
fi

export PATH="$GOPATH/bin:$PATH"
TAGS=with_conntrack,with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api
TARGET=${GOMOBILE_TARGET:-android/arm,android/arm64,android/386,android/amd64}
if [ "${WITH_CRONET:-0}" = 1 ]; then
  TAGS+=,with_cronet
  export CGO_ENABLED=1
  # gomobile builds a generated module outside the source workspace. Local
  # replacements in go.mod are propagated to it; a GOWORK file is not.
  export GOWORK=off
  # Download the matching linker on a fresh CI runner; CRONET_LD can override it.
  CRONET_LD=$(bash ../buildScript/init/cronet_ld.sh)
  export CGO_LDFLAGS="${CGO_LDFLAGS:+$CGO_LDFLAGS }-fuse-ld=$CRONET_LD -Wl,-z,max-page-size=16384"
fi
gomobile bind -v -target="$TARGET" -androidapi 23 -trimpath -ldflags='-s -w' -tags="$TAGS" . || exit 1
rm -r libcore-sources.jar

proj=../app/libs
mkdir -p $proj
cp -f libcore.aar $proj
echo ">> install $(realpath $proj)/libcore.aar"
