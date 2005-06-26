#ifdef WIN32

#include <pdh.h>
#include <pdhmsg.h>

#include "win32bindings.h"
#include "javasigar.h"

/**
 * Set of common utilities for all win32bindings objects
 */

#ifdef __cplusplus
extern "C" {
#endif

void win32_throw_exception(JNIEnv *env, char *msg)
{
    jclass exceptionClass = WIN32_FIND_CLASS("Win32Exception");
    JENV->ThrowNew(env, exceptionClass, msg);
}

void win32_throw_last_error(JNIEnv *env)
{
    char msg[8192];

    FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM |
                  FORMAT_MESSAGE_IGNORE_INSERTS,
                  NULL,
                  GetLastError(),
                  0, /* default language */
                  (LPTSTR)msg,
                  (DWORD)sizeof(msg),
                  NULL);

    win32_throw_exception(env, msg);
}

JNIEXPORT jint SIGAR_JNI(win32_Win32_GetLastError)
(JNIEnv *env, jclass cls)
{
    return GetLastError();
}

JNIEXPORT jstring SIGAR_JNI(win32_Win32_GetErrorMessage)
(JNIEnv *env, jclass cls, jint error)
{
    LPTSTR msg;
    jstring str;

    FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER|
                  FORMAT_MESSAGE_IGNORE_INSERTS|
                  FORMAT_MESSAGE_FROM_SYSTEM,
                  NULL, error, 
                  MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US),
                  (LPTSTR)&msg, 0, NULL);
    str = JENV->NewString(env, (const jchar *)msg, lstrlen(msg));
    LocalFree(msg);
    return str;
}

#ifdef __cplusplus
}
#endif
#endif /* WIN32 */
