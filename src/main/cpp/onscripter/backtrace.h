#ifndef __BACKTRACE_H__
#define __BACKTRACE_H__

size_t get_backtrace(char** out, size_t buffSize);

void print_backtrace(const char* label = NULL);

#endif // __BACKTRACE_H__
