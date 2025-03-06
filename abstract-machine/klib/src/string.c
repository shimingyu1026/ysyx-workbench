#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  size_t count = 0;
  while (*s != '\0')
  {
    count++;
    s++;
  }
  return count;
}

char *strcpy(char *dst, const char *src) {
  if (dst == NULL || src == NULL)
  {
    return NULL;
  }
  char *p = dst;
  while (*src != '\0')
  {
    *dst = *src;
    src++;
    dst++;
  }
  *dst = '\0';
  return p;
}

char *strncpy(char *dst, const char *src, size_t n) {
  panic("Not implemented");
}

char *strcat(char *dst, const char *src) {
  if (dst == NULL || src == NULL)
  {
    return NULL;
  }
  char *p = dst;
  while (*dst != '\0')
  {
    dst++;
  }
  while (*src != '\0')
  {
    *dst = *src;
    dst++;
    src++;
  }
  *dst = '\0';
  return p;
}

int strcmp(const char *s1, const char *s2) {
  while (*s1 && *s2)
  {
    if (*s1 != *s2)
    {
      return *s1 - *s2;
    }
    s1++;
    s2++;
  }
  return *s1 - *s2;
}

int strncmp(const char *s1, const char *s2, size_t n) {
  panic("Not implemented");
}

void *memset(void *s, int c, size_t n) {
  char *p = (char *)s;
  for (size_t i = 0; i < n; i++)
  {
    p[i] = (char)c;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  if (dst == NULL || src == NULL)
  {
    return NULL;
  }
  if ((char *)dst < (const char *)src || (char *)dst >= (const char *)src + n)
  {
    // 目标在源之前，从源开始到源结束复制
    for (size_t i = 0; i < n; i++)
    {
      ((char *)dst)[i] = ((const char *)src)[i];
    }
  }
  else
  {
    // 目标在源之后，从源结束到源开始复制
    for (size_t i = n; i > 0; i--)
    {
      ((char *)dst)[i - 1] = ((const char *)src)[i - 1];
    }
  }

  return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
  panic("Not implemented");
}

int memcmp(const void *s1, const void *s2, size_t n) {
  const char *p1 = (const char *)s1, *p2 = (const char *)s2;
  for (size_t i = 0; i < n; i++)
  {
    if (p1[i] != p2[i])
    {
      return p1[i] - p2[i];
    }
  }
  return 0;
}

#endif
