# JNI 包装构建说明

`llama_jni.cpp` / `logging.h` 复制自 `MiniCPM-V-demo-Android/app/src/main/cpp`(omni/TTS 部分未取),
JNI 符号名已从 `Java_com_example_minicpm_1v_1demo_LlamaEngine_*` 重绑定为
`Java_com_mb_scrapbook_lottery_infer_LlamaEngine_*`。

## 重建命令(仅换包名/升级蓝本时需要)

```bash
NDKBIN="D:/android-sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/windows-x86_64/bin"
LLAMA_SRC="D:/gitprojects/devtoolslab/x-projects/androidlab/MiniCPM-V-Apps/llama.cpp-omni"
BIN="D:/gitprojects/devtoolslab/x-projects/androidlab/MiniCPM-V-Apps/MiniCPM-V-demo-Android/app/.cxx/Release/2b4e6b4m/arm64-v8a/bin"

"$NDKBIN/aarch64-linux-android21-clang++" -shared -fPIC -O2 -std=c++17 \
  -DGGML_SYSTEM_ARCH=ARM -DGGML_CPU_KLEIDIAI=1 -DGGML_OPENMP=1 \
  -I"$LLAMA_SRC" -I"$LLAMA_SRC/common" -I"$LLAMA_SRC/include" \
  -I"$LLAMA_SRC/ggml/include" -I"$LLAMA_SRC/ggml/src" \
  -I"$LLAMA_SRC/tools/mtmd" -I"$LLAMA_SRC/vendor" -I. \
  llama_jni.cpp -L"$BIN" -lllama -lllama-common -lmtmd -llog \
  -o libminicpm_v_demo.so
```

随后 `llvm-strip --strip-unneeded` 连同 llama.cpp 依赖闭包(libllama/libllama-common/libmtmd/
libggml/libggml-base/libggml-cpu/libomp/libc++_shared)拷入 `../jniLibs/arm64-v8a/`。

依赖闭包与版本必须与蓝本 `.cxx` 构建产物同源(ABI 一致);升级 llama.cpp 需同步重编全部 .so。
