#ifndef __SCRIPT_EXCEPTION_H__
#define __SCRIPT_EXCEPTION_H__

#include <iostream>
#include <exception>
#include <string.h>
#include "backtrace.h"

class ScriptException: public std::exception
{
public:
    ScriptException(const char* message = NULL, const char* scriptLine = NULL);
    virtual ~ScriptException() throw();

    virtual const char* what() const throw();
    const char* scriptLine();
    const char* stacktrace();

private:
    char* mMessage;
    char* mScriptLine;
    char* mStacktrace;
};

#endif // __SCRIPT_EXCEPTION_H__