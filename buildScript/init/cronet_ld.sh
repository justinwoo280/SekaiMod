#!/usr/bin/env bash
set -euo pipefail

# Print only the linker path to stdout; build.sh captures it for -fuse-ld.
if [ -n "${CRONET_LD:-}" ]; then
  if [ ! -x "$CRONET_LD" ]; then
    echo "Error: CRONET_LD must point to an executable ld.lld: $CRONET_LD" >&2
    exit 1
  fi
  "$CRONET_LD" --version >&2
  # Keep the ld.lld basename: LLD selects its ELF driver from argv[0].
  printf '%s/%s\n' "$(realpath "$(dirname "$CRONET_LD")")" "$(basename "$CRONET_LD")"
  exit 0
fi

# Match the Chromium toolchain used by the pinned Cronet native archives.
# NDK 26's linker cannot handle their relative C++ vtable relocations.
revision=llvmorg-24-init-3796-g20e97c4b-27
case "$(uname -s)/$(uname -m)" in
  Linux/x86_64)
    platform=Linux_x64
    checksum=968195fa0b49958e066695df2c7c985d169a20fa06eedb8fcef900a6030edcb0
    ;;
  *)
    echo "Error: set CRONET_LD to a Chromium LLVM 24 ld.lld on this host." >&2
    exit 1
    ;;
esac

cache=${CRONET_TOOLCHAIN_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/sekai-cronet}
mkdir -p "$cache"
cache=$(realpath "$cache")
destination="$cache/$revision-$platform"
linker="$destination/bin/ld.lld"

if [ ! -x "$linker" ]; then
  temporary=$(mktemp -d "$cache/.download.XXXXXX")
  trap 'rm -rf "$temporary"' EXIT
  url="https://commondatastorage.googleapis.com/chromium-browser-clang/$platform/clang-$revision.tar.xz"
  echo "Downloading Cronet linker $revision ($platform)..." >&2
  curl --fail --location --retry 3 --output "$temporary/clang.tar.xz" "$url"
  printf '%s  %s\n' "$checksum" "$temporary/clang.tar.xz" | sha256sum --check >&2
  # Only LLD is needed: the NDK still provides the compiler and Android sysroot.
  tar -xJf "$temporary/clang.tar.xz" -C "$temporary" bin/lld bin/ld.lld
  "$temporary/bin/ld.lld" --version >&2
  mkdir -p "$destination/bin"
  mv -f "$temporary/bin/lld" "$destination/bin/lld"
  ln -sf lld "$linker"
fi

"$linker" --version >&2
printf '%s\n' "$linker"
