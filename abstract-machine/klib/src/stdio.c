#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

static void my_itoa_dec(int num, char *str);
static void my_utoa_dec(unsigned int num, char *str);
static void my_itoa_hex(unsigned int num, char *str);
int printf(const char *fmt, ...) {
  //panic("Not implemented");
  va_list args;
  va_start(args, fmt);
  while (*fmt != '\0')
  {
    if (*fmt == '%')
    {
      fmt++;
      switch (*fmt)
      {
      case 'd':
      {
        int num = va_arg(args, int);
        char buf[32];
        my_itoa_dec(num, buf);
        for (int i = 0; i < strlen(buf); i++)
        {
          putch(buf[i]);
        }
        break;
      }
      case 'u':
      {
        unsigned int num = va_arg(args, unsigned int);
        char buf[20];
        my_utoa_dec(num, buf);
        for (int i = 0; i < strlen(buf); i++)
        {
          putch(buf[i]);
        }
        break;
      }
      case 'x':
      {
        unsigned int num = va_arg(args, unsigned int);
        char buf[20];
        my_itoa_hex(num, buf);
        for (int i = 0; i < strlen(buf); i++)
        {
          putch(buf[i]);
        }
        break;
      }
      case 's':
      {
        char *s = va_arg(args, char *);
        for (int i = 0; i < strlen(s); i++)
        {
          putch(s[i]);
        }
        break;
      }
      case 'c':
      {
        char c = (char)va_arg(args, int);
        putch(c);
        break;
      }
      default:
      {
        putch('%');
        putch(*fmt);
        break;
      }
      }
      fmt++;
    }
    else
    {
      putch(*fmt);
      fmt++;
    }
  }

  va_end(args);
  return 0;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char *out, const char *fmt, ...) {
  va_list args;
  va_start(args, fmt);
  char *ptr = out;
  while (*fmt != '\0')
  {
    if (*fmt == '%')
    {
      fmt++; // 跳过%
      if (*fmt == '\0')
        break;      // %后面没有字符
      switch (*fmt) // %后面的字符
      {
      case 'd':
      {
        int num = va_arg(args, int);
        char buf[32];
        my_itoa_dec(num, buf);
        strcpy(ptr, buf);
        ptr += strlen(buf);
        break;
      }
      case 'u':
      {
        unsigned int num = va_arg(args, unsigned int);
        char buf[20];
        my_utoa_dec(num, buf);
        strcpy(ptr, buf);
        ptr += strlen(buf);
        break;
      }
      case 'x':
      {
        unsigned int num = va_arg(args, unsigned int);
        char buf[20];
        my_itoa_hex(num, buf);
        strcpy(ptr, buf);
        ptr += strlen(buf);
        break;
      }
      case 's':
      {
        char *s = va_arg(args, char *);
        strcpy(ptr, s);
        ptr += strlen(s);
        break;
      }
      case 'c':
      {
        char c = (char)va_arg(args, int);
        *ptr++ = c;
        break;
      }
      default:
      {
        *ptr++ = '%';
        *ptr++ = *fmt;
        break;
      }
      }
      fmt++;
    }
    else
    {
      *ptr++ = *fmt++;
    }
  }

  *ptr = '\0'; // 字符串结尾
  va_end(args);
  return ptr - out; // 返回写入的字符数（不包括 '\0'）
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  return 0;
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

// 将整数转换为十进制字符串（支持负数）
static void my_itoa_dec(int num, char *str)
{
  char *ptr = str;
  unsigned int n;
  if (num < 0)
  {
    *ptr++ = '-';
    n = (unsigned int)(-num);
  }
  else
  {
    n = (unsigned int)num;
  }

  char temp[12];
  int i = 0;
  if (n == 0)
    temp[i++] = '0';
  else
    while (n != 0)
    {
      temp[i++] = (n % 10) + '0';
      n /= 10;
    }

  while (i > 0)
    *ptr++ = temp[--i];
  *ptr = '\0';
}

// 将无符号整数转换为十进制字符串
static void my_utoa_dec(unsigned int num, char *str)
{
  char *ptr = str;
  char temp[12];
  int i = 0;
  if (num == 0)
    temp[i++] = '0';
  else
    while (num != 0)
    {
      temp[i++] = (num % 10) + '0';
      num /= 10;
    }

  while (i > 0)
    *ptr++ = temp[--i];
  *ptr = '\0';
}

// 将无符号整数转换为小写十六进制字符串
static void my_itoa_hex(unsigned int num, char *str)
{
  char *ptr = str;
  if (num == 0)
  {
    *ptr++ = '0';
    *ptr = '\0';
    return;
  }

  char temp[8];
  int i = 0;
  while (num != 0)
  {
    unsigned int rem = num % 16;
    temp[i++] = rem < 10 ? (rem + '0') : (rem - 10 + 'a');
    num /= 16;
  }

  while (i > 0)
    *ptr++ = temp[--i];
  *ptr = '\0';
}

#endif
