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
  $BUILD/src \
  $BUILD/lwip-notices

if [ -z "$GOPATH" ]; then
  GOPATH=$(go env GOPATH)
fi

export PATH="$GOPATH/bin:$PATH"
export CGO_ENABLED=1
# gomobile propagates module replacements to its generated module, not go.work.
export GOWORK=off
TAGS=$(cat build_tags)
TARGET=${GOMOBILE_TARGET:-android/arm,android/arm64,android/386,android/amd64}
if [ "${WITH_CRONET:-0}" = 1 ]; then
  TAGS+=,with_cronet
  # The published Cronet fork is not indexed by sum.golang.org. Keep the
  # configured module proxy, but skip checksum database lookups for the fork.
  export GONOSUMDB="${GONOSUMDB:+${GONOSUMDB},}github.com/justinwoo280/*"
  # Download the matching linker on a fresh CI runner; CRONET_LD can override it.
  CRONET_LD=$(bash ../buildScript/init/cronet_ld.sh)
  export CGO_LDFLAGS="${CGO_LDFLAGS:+$CGO_LDFLAGS }-fuse-ld=$CRONET_LD -Wl,-z,max-page-size=16384"
fi
BOX_SOURCE=$(go list -m -f '{{.Dir}}' github.com/sagernet/sing-box)
BOX_VERSION=$(CGO_ENABLED=0 go -C "$BOX_SOURCE" run ./cmd/internal/read_tag)
gomobile bind -v -target="$TARGET" -androidapi 23 -trimpath \
  -ldflags="-s -w -X github.com/sagernet/sing-box/constant.Version=$BOX_VERSION" -tags="$TAGS" . || exit 1
rm -r libcore-sources.jar

# AAR assets are merged into the APK, keeping the native notices with every ABI.
go mod download github.com/justinwoo280/sing-lwip/lib/android_arm64
LWIP_SOURCE=$(go list -m -f '{{.Dir}}' github.com/justinwoo280/sing-lwip)
LWIP_LIBRARY=$(go list -m -f '{{.Dir}}' github.com/justinwoo280/sing-lwip/lib/android_arm64)
NOTICES="$BUILD/lwip-notices/assets/licenses/sing-lwip"
mkdir -p "$NOTICES"
cp "$LWIP_SOURCE/LICENSE" "$LWIP_LIBRARY/LWIP-LICENSE" "$NOTICES/"
cp "$LWIP_SOURCE/third_party/lwip.lock.json" "$NOTICES/"
jar uf libcore.aar -C "$BUILD/lwip-notices" assets

proj=../app/libs
mkdir -p $proj
cp -f libcore.aar $proj
echo ">> install $(realpath $proj)/libcore.aar"
