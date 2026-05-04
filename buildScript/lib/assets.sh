#!/bin/bash

set -e

DIR=app/src/main/assets/sing-box
rm -rf $DIR
mkdir -p $DIR
cd $DIR

get_latest_release() {
  # Use GITHUB_TOKEN when running in Actions to avoid the 60 req/h/IP
  # unauthenticated rate limit (which returns no tag_name and breaks
  # the download URL with an empty version).
  local auth_header=()
  if [ -n "$GITHUB_TOKEN" ]; then
    auth_header=(-H "Authorization: Bearer $GITHUB_TOKEN")
  fi
  curl --silent "${auth_header[@]}" "https://api.github.com/repos/$1/releases/latest" | # Get latest release from GitHub api
    grep '"tag_name":' |                                            # Get tag line
    sed -E 's/.*"([^"]+)".*/\1/'                                    # Pluck JSON value
}

require_version() {
  if [ -z "$2" ]; then
    echo "ERROR: failed to fetch latest release for $1 (likely GitHub API rate limit; set GITHUB_TOKEN)" >&2
    exit 1
  fi
}

####
VERSION_GEOIP=`get_latest_release "SagerNet/sing-geoip"`
echo VERSION_GEOIP=$VERSION_GEOIP
require_version "SagerNet/sing-geoip" "$VERSION_GEOIP"
echo -n $VERSION_GEOIP > geoip.version.txt
curl -fLSsO https://github.com/SagerNet/sing-geoip/releases/download/$VERSION_GEOIP/geoip.db
xz -9 geoip.db

####
VERSION_GEOSITE=`get_latest_release "SagerNet/sing-geosite"`
echo VERSION_GEOSITE=$VERSION_GEOSITE
require_version "SagerNet/sing-geosite" "$VERSION_GEOSITE"
echo -n $VERSION_GEOSITE > geosite.version.txt
curl -fLSsO https://github.com/SagerNet/sing-geosite/releases/download/$VERSION_GEOSITE/geosite.db
xz -9 geosite.db
