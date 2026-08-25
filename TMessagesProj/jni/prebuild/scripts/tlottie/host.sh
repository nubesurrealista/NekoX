#!/bin/sh
#
# Builds tlottie on the host. Expects the tlottie source to be at
# ./tlottie relative to the repo root (or TLOTTIE_DIR override), and
# outputs to the repo root (or OUT_ROOT override).
# Requires a working Rust toolchain with Android targets installed.

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
prebuild_root=$(cd "$(dirname "$0")/../../../prebuild" && pwd)
tlottie_dir="${TLOTTIE_DIR:-"$script_dir/../../../tlottie"}"
manifest="$tlottie_dir/Cargo.toml"
profile=release-nostd
features=cpu,no-std,c-api
out_root="${OUT_ROOT:-$script_dir/../../../prebuild}"

if [ ! -f "$manifest" ]; then
  echo "error: tlottie Cargo.toml not found at $manifest" >&2
  exit 1
fi

target_dir="$prebuild_root/tlottie-build"
mkdir -p "$target_dir"
export CARGO_TARGET_DIR="$target_dir"

rust_sysroot=$(rustc --print sysroot)

unset RUSTFLAGS
CARGO_ENCODED_RUSTFLAGS=
append_rustflag() {
  if [ -n "$CARGO_ENCODED_RUSTFLAGS" ]; then
    CARGO_ENCODED_RUSTFLAGS="$CARGO_ENCODED_RUSTFLAGS$(printf '\037')"
  fi
  CARGO_ENCODED_RUSTFLAGS="$CARGO_ENCODED_RUSTFLAGS$1"
}
append_rustflag "--remap-path-prefix=$tlottie_dir=$tlottie_dir"
append_rustflag "--remap-path-prefix=$target_dir=$target_dir"
append_rustflag "--remap-path-prefix=$rust_sysroot=$rust_sysroot"
#append_rustflag "--remap-path-prefix=$CARGO_HOME=$CARGO_HOME"
export CARGO_ENCODED_RUSTFLAGS

echo "Building tlottie Android archives"
echo "  source:   $tlottie_dir"
echo "  rustc:    $(rustc --version)"
echo "  profile:  $profile"
echo "  features: $features"

for target_spec in \
  arm64-v8a:aarch64-linux-android \
  armeabi-v7a:armv7-linux-androideabi \
  x86:i686-linux-android \
  x86_64:x86_64-linux-android
do
  abi=${target_spec%%:*}
  rust_target=${target_spec#*:}
  destination="$out_root/$abi/libtlottie.a"
  mkdir -p "$out_root/$abi"

  echo "Building $abi ($rust_target)"
  cargo rustc \
    --manifest-path "$manifest" \
    --locked \
    --profile "$profile" \
    --target "$rust_target" \
    --lib \
    --no-default-features \
    --features "$features" \
    --crate-type staticlib \
    -- \
    -C metadata=tlottie-staticlib

  artifact="$target_dir/$rust_target/$profile/libtlottie.a"
  if [ ! -f "$artifact" ]; then
    echo "error: cargo did not emit $artifact" >&2
    exit 1
  fi
  if [ ! -d "$out_root/$abi" ]; then
    mkdir -p "$out_root/$abi" || exit 1
#    echo "error: output directory does not exist: $out_root/$abi" >&2
#    exit 1
  fi

  tmp=$(mktemp "$out_root/$abi/.libtlottie.a.XXXXXX")
  cp "$artifact" "$tmp"
  chmod 0644 "$tmp"
  mv -f "$tmp" "$destination"

  bytes=$(wc -c < "$destination" | tr -d ' ')
  echo "Wrote $destination ($bytes bytes)"
done

echo "All tlottie Android archives are ready."
