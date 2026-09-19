#!/bin/bash

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
if [ "${WITH_CRONET:-0}" = 1 ]; then
  # The published Cronet archive currently covers Android arm64 only.
  TARGET=${GOMOBILE_TARGET:-android/arm64}
  if [ "$TARGET" != "android/arm64" ]; then
    echo "Error: Browser Cronet is currently available only for android/arm64."
    echo "       Set GOMOBILE_TARGET=android/arm64 or build matching Cronet archives first."
    exit 1
  fi
  TAGS+=,with_cronet
  export CGO_ENABLED=1
  # gomobile builds a generated module outside the source workspace. Local
  # replacements in go.mod are propagated to it; a GOWORK file is not.
  export GOWORK=off
  # Cronet's relative C++ vtables need a newer linker than NDK 26 supplies.
  CRONET_LD=${CRONET_LD:-"$PWD/../../cronet-go/naiveproxy/src/third_party/llvm-build/Release+Asserts/bin/ld.lld"}
  if [ ! -x "$CRONET_LD" ]; then
    echo "Error: build the Cronet toolchain first, or set CRONET_LD to its ld.lld."
    exit 1
  fi
  export CGO_LDFLAGS="${CGO_LDFLAGS:+$CGO_LDFLAGS }-fuse-ld=$CRONET_LD -Wl,-z,max-page-size=16384"
else
  TARGET=${GOMOBILE_TARGET:-android/arm,android/arm64,android/386,android/amd64}
fi
gomobile bind -v -target="$TARGET" -androidapi 23 -trimpath -ldflags='-s -w' -tags="$TAGS" . || exit 1
rm -r libcore-sources.jar

proj=../app/libs
mkdir -p $proj
cp -f libcore.aar $proj
echo ">> install $(realpath $proj)/libcore.aar"
