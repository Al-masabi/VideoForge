#!/usr/bin/env bash
set -euo pipefail

FFMPEG_VERSION="7.1"
API=29

MODULE_DIR="$(cd "$(dirname "$0")/../engine/ffmpeg-native" && pwd)"
WORK_DIR="$(pwd)/ffmpeg-build"

NDK="${ANDROID_NDK_HOME:-${NDK_ROOT:-}}"

if [ -z "$NDK" ]; then
    echo "Set ANDROID_NDK_HOME to your NDK path"
    exit 1
fi

HOST_TAG="linux-x86_64"
if uname | grep -qi "darwin"; then
    HOST_TAG="darwin-x86_64"
fi

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
JOBS="$( (sysctl -n hw.ncpu 2>/dev/null || nproc) )"

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

if [ ! -d "ffmpeg-$FFMPEG_VERSION" ]; then
    echo "Downloading FFmpeg $FFMPEG_VERSION..."
    curl -L "https://ffmpeg.org/releases/ffmpeg-$FFMPEG_VERSION.tar.gz" -o ffmpeg.tar.gz
    tar xf ffmpeg.tar.gz
fi

if [ ! -d "x264-stable" ]; then
    echo "Downloading x264..."
    curl -L "https://code.videolan.org/videolan/x264/-/archive/stable/x264-stable.tar.gz" -o x264.tar.gz
    tar xf x264.tar.gz
fi

build_x264() {
    local ABI="$1"
    local HOST_ARCH="$2"
    local CLANG_TRIPLE="$3"
    local BINUTILS_TRIPLE="$4"

    local PREFIX="$WORK_DIR/x264/$ABI"

    echo "Building x264 for $ABI..."

    pushd x264-stable > /dev/null
    make distclean > /dev/null 2>&1 || true

    CC="$TOOLCHAIN/bin/${CLANG_TRIPLE}${API}-clang" \
    AR="$TOOLCHAIN/bin/llvm-ar" \
    RANLIB="$TOOLCHAIN/bin/llvm-ranlib" \
    ./configure \
        --prefix="$PREFIX" \
        --host="$HOST_ARCH" \
        --cross-prefix="$TOOLCHAIN/bin/${BINUTILS_TRIPLE}-" \
        --sysroot="$TOOLCHAIN/sysroot" \
        --enable-static \
        --enable-pic \
        --disable-cli \
        --disable-asm

    make -j"$JOBS"
    make install
    popd > /dev/null
}

build_ffmpeg() {
    local ABI="$1"
    local ARCH="$2"
    local CLANG_TRIPLE="$3"
    local EXTRA_FLAGS="$4"

    local PREFIX="$WORK_DIR/prebuilt/$ABI"
    local X264_PREFIX="$WORK_DIR/x264/$ABI"

    echo "=========================================="
    echo "Building FFmpeg for $ABI (with libx264 CRF)"
    echo "=========================================="

    pushd "ffmpeg-$FFMPEG_VERSION" > /dev/null
    make distclean > /dev/null 2>&1 || true

    ./configure \
        --prefix="$PREFIX" \
        --target-os=android \
        --arch="$ARCH" \
        --cc="$TOOLCHAIN/bin/${CLANG_TRIPLE}${API}-clang" \
        --cxx="$TOOLCHAIN/bin/${CLANG_TRIPLE}${API}-clang++" \
        --enable-cross-compile \
        --enable-shared \
        --disable-static \
        --disable-programs \
        --disable-doc \
        --disable-debug \
        --disable-autodetect \
        --disable-everything \
        --enable-gpl \
        --enable-protocol=file,pipe \
        --enable-demuxer=mov,mp4,m4a,3gp,3g2,mj2,matroska,avi,flv,mpegts,ogg,wav \
        --enable-muxer=mp4,mov,matroska \
        --enable-parser=h264,hevc,aac,ac3,eac3,opus,vorbis,vp8,vp9,av1,mpegaudio,ass,srt,webvtt \
        --enable-decoder=h264,hevc,mpeg4,aac,mp3,opus,vorbis \
        --enable-swscale \
        --enable-swresample \
        --enable-libx264 \
        --enable-encoder=libx264 \
        --extra-cflags="-I$X264_PREFIX/include -O2" \
        --extra-ldflags="-L$X264_PREFIX/lib" \
        --extra-libs="-lx264 -lm" \
        $EXTRA_FLAGS

    make -j"$JOBS"
    make install
    popd > /dev/null

    mkdir -p "$MODULE_DIR/src/main/jniLibs/$ABI"
    cp "$PREFIX/lib/"*.so "$MODULE_DIR/src/main/jniLibs/$ABI/"

    mkdir -p "$MODULE_DIR/src/main/cpp/ffmpeg/$ABI/lib"
    cp "$PREFIX/lib/"*.so "$MODULE_DIR/src/main/cpp/ffmpeg/$ABI/lib/"
    cp -r "$PREFIX/include" "$MODULE_DIR/src/main/cpp/ffmpeg/$ABI/include"

    echo "Done: $ABI"
}

build_x264 "arm64-v8a"   "aarch64-linux-android" "aarch64-linux-android" "aarch64-linux-android"
build_x264 "armeabi-v7a" "arm-linux-androideabi" "armv7a-linux-androideabi" "arm-linux-androideabi"

build_ffmpeg "arm64-v8a"   "aarch64" "aarch64-linux-android" ""
build_ffmpeg "armeabi-v7a" "arm"     "armv7a-linux-androideabi" "--enable-neon"

echo ""
echo "FFmpeg + libx264 ready. Rebuild the app to compile the native bridge."