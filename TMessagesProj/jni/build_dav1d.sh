#!/bin/bash

set -e

# From upstream repo

PREFIX="$(pwd)/dav1d/build"
rm -rf "$PREFIX"
mkdir -p "$PREFIX"

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

echo "Building dav1d into $PREFIX"

pushd dav1d

cat > "$TMPDIR/cross.ini" <<EOF
[binaries]
c = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang'
ar = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android-ar'

[host_machine]
system = 'android'
cpu_family = 'aarch64'
cpu = 'arm64'
endian = 'little'
EOF

meson setup builddir-arm64 \
  --wipe \
  --prefix "$PREFIX/arm64-v8a" \
  --libdir="lib" \
  --includedir="include" \
  --buildtype=release -Denable_tests=false -Denable_tools=false -Ddefault_library=static \
  --cross-file "$TMPDIR/cross.ini"
ninja -C builddir-arm64
ninja -C builddir-arm64 install

cat > "$TMPDIR/cross.ini" <<EOF
[binaries]
c = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi21-clang'
ar = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/arm-linux-androideabi-ar'

[host_machine]
system = 'android'
cpu_family = 'arm'
cpu = 'armv7'
endian = 'little'
EOF

meson setup builddir-armv7 \
  --wipe \
  --prefix "$PREFIX/armeabi-v7a" \
  --libdir="lib" \
  --includedir="include" \
  --buildtype=release -Denable_tests=false -Denable_tools=false -Ddefault_library=static \
  --cross-file "$TMPDIR/cross.ini" \
  -Dc_args="-DDAV1D_NO_GETAUXVAL"
ninja -C builddir-armv7
ninja -C builddir-armv7 install

cat > "$TMPDIR/cross.ini" <<EOF
[binaries]
c = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android21-clang'
ar = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android-ar'

[host_machine]
system = 'android'
cpu_family = 'x86'
cpu = 'i686'
endian = 'little'
EOF

meson setup builddir-x86 \
  --wipe \
  --prefix "$PREFIX/x86" \
  --libdir="lib" \
  --includedir="include" \
  --buildtype=release -Denable_tests=false -Denable_tools=false -Ddefault_library=static \
  --cross-file "$TMPDIR/cross.ini"
ninja -C builddir-x86
ninja -C builddir-x86 install

cat > "$TMPDIR/cross.ini" <<EOF
    [binaries]
    c = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android21-clang'
    ar = '${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android-ar'

    [host_machine]
    system = 'android'
    cpu_family = 'x86_64'
    cpu = 'x86_64'
    endian = 'little'
EOF

meson setup builddir-x86_64 \
  --wipe \
  --prefix "$PREFIX/x86_64" \
  --libdir="lib" \
  --includedir="include" \
  --buildtype=release -Denable_tests=false -Denable_tools=false -Ddefault_library=static \
  --cross-file "$TMPDIR/cross.ini"
ninja -C builddir-x86_64
ninja -C builddir-x86_64 install

popd

