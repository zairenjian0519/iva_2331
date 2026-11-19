#define _GNU_SOURCE

#include <dlfcn.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

static void* (*real_dlopen)(const char*, int);
static pthread_once_t dlopen_once = PTHREAD_ONCE_INIT;

static void resolve_dlopen(void) {
  real_dlopen = dlsym(RTLD_NEXT, "dlopen");
}

void* dlopen(const char* filename, int flags) {
  pthread_once(&dlopen_once, resolve_dlopen);
  if (real_dlopen == NULL) {
    return NULL;
  }

  const char* redirect = getenv("RKNNRT_REDIRECT");
  if (redirect != NULL && filename != NULL &&
      (strcmp(filename, "/usr/lib/librknnrt.so") == 0 ||
       strcmp(filename, "/lib/librknnrt.so") == 0)) {
    return real_dlopen(redirect, flags);
  }
  return real_dlopen(filename, flags);
}
