#ifdef ANDROID
#ifndef __JNI_HELPER_H__
#define __JNI_HELPER_H__

#include <jni.h>
#include <android/log.h>

#ifndef SDL_JAVA_PACKAGE_PATH
#error You have to define SDL_JAVA_PACKAGE_PATH to your package path with dots replaced with underscores, for example "com_example_SanAngeles"
#endif
#define JAVA_EXPORT_NAME2(name,package) Java_##package##_##name
#define JAVA_EXPORT_NAME1(name,package) JAVA_EXPORT_NAME2(name,package)
#define JAVA_EXPORT_NAME(name) JAVA_EXPORT_NAME1(name,SDL_JAVA_PACKAGE_PATH)

class JNIWrapper {
public:
    JNIWrapper(JavaVM *vm)
        : mAttached(false)
        , mJniVM(vm)
        , env(NULL)
    {
        switch (mJniVM->GetEnv((void**)&env, JNI_VERSION_1_6))
        {
            case JNI_OK:
                break;
            case JNI_EDETACHED:
                if (mJniVM->AttachCurrentThread(&env, NULL) != 0) {
                    __android_log_print(ANDROID_LOG_ERROR, "JNIWrapper", "JNI cannot attach to thread");
                }
                mAttached = true;
                break;
            case JNI_EVERSION:
                __android_log_print(ANDROID_LOG_ERROR, "JNIWrapper", "Bad java version");
                break;
        }
    }
    ~JNIWrapper()
    {
        if (mAttached) {
            mJniVM->DetachCurrentThread();
        }
    }

    JNIEnv * env;

private:
    bool mAttached;
    JavaVM *mJniVM;
};

#endif // __JNI_HELPER_H__
#endif // ANDROID