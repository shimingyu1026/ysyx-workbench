#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>
#include <stdint.h>
#define FLOCK(f) int __need_unlock = ((f)->lock >= 0 ? __lockfile((f)) : 0)
#define INT_MAX __INT_MAX__
#define MAX(a, b) ((a) > (b) ? (a) : (b))
#define MIN(a, b) ((a) < (b) ? (a) : (b))
#define ALT_FORM (1U << ('#' - ' '))
#define ZERO_PAD (1U << ('0' - ' '))
#define LEFT_ADJ (1U << ('-' - ' '))
#define PAD_POS (1U << (' ' - ' '))
#define MARK_POS (1U << ('+' - ' '))
#define GROUPED (1U << ('\'' - ' '))
#define FLAGMASK (ALT_FORM | ZERO_PAD | LEFT_ADJ | PAD_POS | MARK_POS | GROUPED)
enum
{
  BARE,
  LPRE,
  LLPRE,
  HPRE,
  HHPRE,
  BIGLPRE,
  ZTPRE,
  JPRE,
  STOP,
  PTR,
  INT,
  UINT,
  ULLONG,
  LONG,
  ULONG,
  SHORT,
  USHORT,
  CHAR,
  UCHAR,
  LLONG,
  SIZET,
  IMAX,
  UMAX,
  PDIFF,
  UIPTR,
  DBL,
  LDBL,
  NOARG,
  MAXSTATE
};
#define S(x) [(x) - 'A']
static const unsigned char states[]['z' - 'A' + 1] = {
    {
        /* 0: bare types */
        S('d') = INT,
        S('i') = INT,
        S('o') = UINT,
        S('u') = UINT,
        S('x') = UINT,
        S('X') = UINT,
        S('e') = DBL,
        S('f') = DBL,
        S('g') = DBL,
        S('a') = DBL,
        S('E') = DBL,
        S('F') = DBL,
        S('G') = DBL,
        S('A') = DBL,
        S('c') = INT,
        S('C') = UINT,
        S('s') = PTR,
        S('S') = PTR,
        S('p') = UIPTR,
        S('n') = PTR,
        S('m') = NOARG,
        S('l') = LPRE,
        S('h') = HPRE,
        S('L') = BIGLPRE,
        S('z') = ZTPRE,
        S('j') = JPRE,
        S('t') = ZTPRE,
    },
    {
        /* 1: l-prefixed */
        S('d') = LONG,
        S('i') = LONG,
        S('o') = ULONG,
        S('u') = ULONG,
        S('x') = ULONG,
        S('X') = ULONG,
        S('e') = DBL,
        S('f') = DBL,
        S('g') = DBL,
        S('a') = DBL,
        S('E') = DBL,
        S('F') = DBL,
        S('G') = DBL,
        S('A') = DBL,
        S('c') = UINT,
        S('s') = PTR,
        S('n') = PTR,
        S('l') = LLPRE,
    },
    {
        /* 2: ll-prefixed */
        S('d') = LLONG,
        S('i') = LLONG,
        S('o') = ULLONG,
        S('u') = ULLONG,
        S('x') = ULLONG,
        S('X') = ULLONG,
        S('n') = PTR,
    },
    {
        /* 3: h-prefixed */
        S('d') = SHORT,
        S('i') = SHORT,
        S('o') = USHORT,
        S('u') = USHORT,
        S('x') = USHORT,
        S('X') = USHORT,
        S('n') = PTR,
        S('h') = HHPRE,
    },
    {
        /* 4: hh-prefixed */
        S('d') = CHAR,
        S('i') = CHAR,
        S('o') = UCHAR,
        S('u') = UCHAR,
        S('x') = UCHAR,
        S('X') = UCHAR,
        S('n') = PTR,
    },
    {
        /* 5: L-prefixed */
        S('e') = LDBL,
        S('f') = LDBL,
        S('g') = LDBL,
        S('a') = LDBL,
        S('E') = LDBL,
        S('F') = LDBL,
        S('G') = LDBL,
        S('A') = LDBL,
        S('n') = PTR,
    },
    {
        /* 6: z- or t-prefixed (assumed to be same size) */
        S('d') = PDIFF,
        S('i') = PDIFF,
        S('o') = SIZET,
        S('u') = SIZET,
        S('x') = SIZET,
        S('X') = SIZET,
        S('n') = PTR,
    },
    {
        /* 7: j-prefixed */
        S('d') = IMAX,
        S('i') = IMAX,
        S('o') = UMAX,
        S('u') = UMAX,
        S('x') = UMAX,
        S('X') = UMAX,
        S('n') = PTR,
    }};
#define OOB(x) ((unsigned)(x) - 'A' > 'z' - 'A')
#define ULONG_MAX (__LONG_MAX__ * 2UL + 1UL)
union arg
{
  uintmax_t i;
  long double f;
  void *p;
};
static void pop_arg(union arg *arg, int type, va_list *ap)
{
  switch (type)
  {
  case PTR:
    arg->p = va_arg(*ap, void *);
    break;
  case INT:
    arg->i = va_arg(*ap, int);
    break;
  case UINT:
    arg->i = va_arg(*ap, unsigned int);
    break;
  case LONG:
    arg->i = va_arg(*ap, long);
    break;
  case ULONG:
    arg->i = va_arg(*ap, unsigned long);
    break;
  case ULLONG:
    arg->i = va_arg(*ap, unsigned long long);
    break;
  case SHORT:
    arg->i = (short)va_arg(*ap, int);
    break;
  case USHORT:
    arg->i = (unsigned short)va_arg(*ap, int);
    break;
  case CHAR:
    arg->i = (signed char)va_arg(*ap, int);
    break;
  case UCHAR:
    arg->i = (unsigned char)va_arg(*ap, int);
    break;
  case LLONG:
    arg->i = va_arg(*ap, long long);
    break;
  case SIZET:
    arg->i = va_arg(*ap, size_t);
    break;
  case IMAX:
    arg->i = va_arg(*ap, intmax_t);
    break;
  case UMAX:
    arg->i = va_arg(*ap, uintmax_t);
    break;
  case PDIFF:
    arg->i = va_arg(*ap, ptrdiff_t);
    break;
  case UIPTR:
    arg->i = (uintptr_t)va_arg(*ap, void *);
    break;
    /* case DBL: */
    /*   arg->f = va_arg(*ap, double); */
    /*   break; */
    /* case LDBL: */
    /*   arg->f = va_arg(*ap, long double); */
  }
}
static void out(const char *s, size_t l)
{
  for (; l; l--, s++)
    putch(*s);
}
static void pad(char c, int w, int l, int fl)
{
  char pad[256];
  if (fl & (LEFT_ADJ | ZERO_PAD) || l >= w)
    return;
  l = w - l;
  memset(pad, c, l > sizeof pad ? sizeof pad : l);
  for (; l >= sizeof pad; l -= sizeof pad)
    out(pad, sizeof pad);
  out(pad, l);
}

int my_isdigit(int c) { return c >= '0' && c <= '9'; }

static int getint(char **s)
{
  int i;
  for (i = 0; my_isdigit(**s); (*s)++)
  {
    if (i > INT_MAX / 10U || **s - '0' > INT_MAX - 10 * i)
      i = -1;
    else
      i = 10 * i + (**s - '0');
  }
  return i;
}
static const char xdigits[16] = {"0123456789ABCDEF"};
static char *fmt_x(uintmax_t x, char *s, int lower)
{
  for (; x; x >>= 4)
    *--s = xdigits[(x & 15)] | lower;
  return s;
}

static char *fmt_o(uintmax_t x, char *s)
{
  for (; x; x >>= 3)
    *--s = '0' + (x & 7);
  return s;
}

static char *fmt_u(uintmax_t x, char *s)
{
  unsigned long y;
  for (; x > ULONG_MAX; x /= 10)
    *--s = '0' + x % 10;
  for (y = x; y; y /= 10)
    *--s = '0' + y % 10;
  return s;
}
static int printf_core(const char *fmt, va_list *ap, union arg *nl_arg,
                       int *nl_type)
{
  char *a, *z, *s = (char *)fmt;
  unsigned l10n = 0, fl;
  int w, p, xp;
  union arg arg;
  int argpos;
  unsigned st, ps;
  int cnt = 0, l = 0;
  size_t i;
  char buf[sizeof(uintmax_t) * 3];
  const char *prefix;
  int t, pl;
  int wc[2];
  for (;;)
  {
    if (l > INT_MAX - cnt)
      goto overflow;
    cnt += l;
    if (!*s)
      break;
    for (a = s; *s && *s != '%'; s++)
      ;
    for (z = s; s[0] == '%' && s[1] == '%'; z++, s += 2)
      ;
    if (z - a > INT_MAX - cnt)
      goto overflow;
    l = z - a;
    out(a, l);
    if (l)
      continue;
    if (my_isdigit(s[1]) && s[2] == '$')
    {
      l10n = 1;
      argpos = s[1] - '0';
      s += 3;
    }
    else
    {
      argpos = -1;
      s++;
    }
    for (fl = 0; (unsigned)*s - ' ' < 32 && (FLAGMASK & (1U << (*s - ' '))); s++)
      fl |= 1U << (*s - ' ');
    if (*s == '*')
    {
      if (my_isdigit(s[1]) && s[2] == '$')
      {
        l10n = 1;

        w = nl_arg[s[1] - '0'].i;
        s += 3;
      }
      else if (!l10n)
      {
        w = va_arg(*ap, int);
        s++;
      }
      else
        goto inval;
      if (w < 0)
        fl |= LEFT_ADJ, w = -w;
    }
    else if ((w = getint(&s)) < 0)
      goto overflow;
    if (*s == '.' && s[1] == '*')
    {
      if (my_isdigit(s[2]) && s[3] == '$')
      {
        p = nl_arg[s[2] - '0'].i;
        s += 4;
      }
      else if (!l10n)
      {
        p = va_arg(*ap, int);
        s += 2;
      }
      else
        goto inval;
      xp = (p >= 0);
    }
    else if (*s == '.')
    {
      s++;
      p = getint(&s);
      xp = 1;
    }
    else
    {
      p = -1;
      xp = 0;
    }
    st = 0;
    do
    {
      if (OOB(*s))
        goto inval;
      ps = st;
      st = states[st] S(*s++);
    } while (st - 1 < STOP);
    if (!st)
      goto inval;

    /* Check validity of argument type (nl/normal) */
    if (st == NOARG)
    {
      if (argpos >= 0)
        goto inval;
    }
    else
    {
      if (argpos >= 0)
      {
        if (!1)
          nl_type[argpos] = st;
        else
          arg = nl_arg[argpos];
      }
      else if (1)
        pop_arg(&arg, st, ap);
      else
        return 0;
    }
    if (!1)
      continue;
    z = buf + sizeof(buf);
    prefix = "-+   0X0x";
    pl = 0;
    t = s[-1];

    if (ps && (t & 15) == 3)
      t &= ~32;
    if (fl & LEFT_ADJ)
      fl &= ~ZERO_PAD;
    switch (t)
    {
    case 'n':
      switch (ps)
      {
      case BARE:
        *(int *)arg.p = cnt;
        break;
      case LPRE:
        *(long *)arg.p = cnt;
        break;
      case LLPRE:
        *(long long *)arg.p = cnt;
        break;
      case HPRE:
        *(unsigned short *)arg.p = cnt;
        break;
      case HHPRE:
        *(unsigned char *)arg.p = cnt;
        break;
      case ZTPRE:
        *(size_t *)arg.p = cnt;
        break;
      case JPRE:
        *(uintmax_t *)arg.p = cnt;
        break;
      }
      continue;
    case 'p':
      p = MAX(p, 2 * sizeof(void *));
      t = 'x';
      fl |= ALT_FORM;
    case 'x':
    case 'X':
      a = fmt_x(arg.i, z, t & 32);
      if (arg.i && (fl & ALT_FORM))
        prefix += (t >> 4), pl = 2;
      if (0)
      {
      case 'o':
        a = fmt_o(arg.i, z);
        if ((fl & ALT_FORM) && p < z - a + 1)
          p = z - a + 1;
      }
      if (0)
      {
      case 'd':
      case 'i':
        pl = 1;
        if (arg.i > INTMAX_MAX)
        {
          arg.i = -arg.i;
        }
        else if (fl & MARK_POS)
        {
          prefix++;
        }
        else if (fl & PAD_POS)
        {
          prefix += 2;
        }
        else
          pl = 0;
      case 'u':
        a = fmt_u(arg.i, z);
      }
      if (xp && p < 0)
        goto overflow;
      if (xp)
        fl &= ~ZERO_PAD;
      if (!arg.i && !p)
      {
        a = z;
        break;
      }
      p = MAX(p, z - a + !arg.i);
      break;
    narrow_c:
    case 'c':
      *(a = z - (p = 1)) = arg.i;
      fl &= ~ZERO_PAD;
      break;
      /*    case 'm': */
      /* if (1) a = strerror(errno); else */
    case 's':
      a = arg.p ? arg.p : "(null)";
      z = a + strlen(a);
      if (p < 0 && *z)
        goto overflow;
      p = z - a;
      fl &= ~ZERO_PAD;
      break;
    case 'C':
      if (!arg.i)
        goto narrow_c;
      wc[0] = arg.i;
      wc[1] = 0;
      arg.p = wc;
      p = -1;
      /* case 'S': */
      /* 	ws = arg.p; */
      /* 	for (i=l=0; i<p && *ws && (l=wctomb(mb, *ws++))>=0 && l<=p-i;
       * i+=l); */
      /* 	if (l<0) return -1; */
      /* 	if (i > INT_MAX) goto overflow; */
      /* 	p = i; */
      /* 	pad(f, ' ', w, p, fl); */
      /* 	ws = arg.p; */
      /* 	for (i=0; i<0U+p && *ws && i+(l=wctomb(mb, *ws++))<=p; i+=l) */
      /* 		out(f, mb, l); */
      /* 	pad(f, ' ', w, p, fl^LEFT_ADJ); */
      /* 	l = w>p ? w : p; */
      /* 	continue; */
      /* case 'e': case 'f': case 'g': case 'a': */
      /* case 'E': case 'F': case 'G': case 'A': */
      /* 	if (xp && p<0) goto overflow; */
      /* 	l = fmt_fp(f, arg.f, w, p, fl, t); */
      /* 	if (l<0) goto overflow; */
      /* 	continue; */
    }
    if (p < z - a)
      p = z - a;
    if (p > INT_MAX - pl)
      goto overflow;
    if (w < pl + p)
      w = pl + p;
    if (w > INT_MAX - cnt)
      goto overflow;
    pad(' ', w, pl + p, fl);
    out(prefix, pl);
    pad('0', w, pl + p, fl ^ ZERO_PAD);
    pad('0', p, z - a, 0);
    out(a, z - a);
    pad(' ', w, pl + p, fl ^ LEFT_ADJ);

    l = w;
  }
  if (1)
    return cnt;
  if (!l10n)
    return 0;
  for (i = 1; i <= 9 && nl_type[i]; i++)
    pop_arg(nl_arg + i, nl_type[i], ap);
  for (; i <= 9 && !nl_type[i]; i++)
    ;
  if (i <= 9)
    goto inval;
  return 1;
inval:
  return -1;
overflow:
  return -1;
}

int vfprintf_am(const char *restrict fmt, va_list ap)
{
  va_list ap2;
  int nl_type[9 + 1] = {0};
  union arg nl_arg[9 + 1];
  int ret;

  va_copy(ap2, ap);

  ret = printf_core(fmt, &ap2, nl_arg, nl_type);

  return ret;
}

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

static void my_itoa_dec(int num, char *str);
static void my_utoa_dec(unsigned int num, char *str);
static void my_itoa_hex(unsigned int num, char *str);
int printf(const char *fmt, ...)
{
  int ret;
  va_list ap;
  va_start(ap, fmt);
  ret = vfprintf_am(fmt, ap);
  va_end(ap);
  return ret;
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
