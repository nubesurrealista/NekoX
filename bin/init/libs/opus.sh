#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/third_party/xiph/ogg
git submodule update --init TMessagesProj/jni/third_party/xiph/opus
git submodule update --init TMessagesProj/jni/third_party/xiph/opusfile

./TMessagesProj/jni/ffmpeg/build_opus.sh

for a in arm64-v8a armeabi-v7a x86 x86_64; do
mv -fv TMessagesProj/jni/ffmpeg/build/opus/$a/libopus.a TMessagesProj/jni/ffmpeg/$a/
done