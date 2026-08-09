#!/bin/bash

source "bin/init/env.sh"

cd TMessagesProj/jni || exit 1
git submodule update --init tdlib

cd tde2e
git reset --hard
git clean -fdx
cd ..

./patch_tdlib.sh
./build_tde2e.sh || exit 1
