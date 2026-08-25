#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/tlottie_lib

for a in arm64-v8a armeabi-v7a x86 x86_64; do
mkdir -p TMessagesProj/jni/tlottie_lib/$a
done

./TMessagesProj/jni/prebuild/scripts/tlottie/host.sh
