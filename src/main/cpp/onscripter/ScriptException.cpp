#include "ScriptException.h"

#define BACKTRACE_MAX_LENGTH 2048

ScriptException::ScriptException(const char* message, const char* scriptLine)
    : mMessage(NULL)
    , mScriptLine(NULL)
    , mStacktrace(NULL)
{
    if (message) {
        int len = strlen(message);
        mMessage = new char[len + 1];
        strncpy(mMessage, message, len);
        mMessage[len] = '\0';

        if (scriptLine) {
            len = strlen(scriptLine);
            mScriptLine = new char[len + 1];
            strncpy(mScriptLine, scriptLine, len);
            mScriptLine[len] = '\0';
        }
    }
    mStacktrace = new char[BACKTRACE_MAX_LENGTH];
    get_backtrace(&mStacktrace, BACKTRACE_MAX_LENGTH);
}

ScriptException::~ScriptException() throw() {
    if (mMessage) {
        delete[] mMessage;
        mMessage = NULL;
    }
    if (mScriptLine) {
        delete[] mScriptLine;
        mScriptLine = NULL;
    }
    if (mStacktrace) {
        delete[] mStacktrace;
        mStacktrace = NULL;
    }
}

const char* ScriptException::what() const throw() {
    return mMessage;
}

const char* ScriptException::scriptLine() {
    return mScriptLine;
}

const char* ScriptException::stacktrace() {
    return mStacktrace;
}
