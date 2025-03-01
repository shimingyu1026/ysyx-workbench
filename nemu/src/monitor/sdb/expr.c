/***************************************************************************************
 * Copyright (c) 2014-2022 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan
 *PSL v2. You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
 *KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

#include <isa.h>
// #include "/home/smy/Seafile/ysyx-workbench/nemu/include/isa.h"
/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <debug.h>
#include <memory/vaddr.h>
#include <regex.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

enum
{
  TK_NOTYPE = 0,
  TK_EQ = 1,
  TK_NUM = 2,
  TK_UEQ = 3,
  TK_AND = 4,
  TK_HEX = 5,
  TK_REG = 6,
  TK_POINT = 7
  /* TODO: Add more token types */

};

static struct rule
{
  const char *regex;
  int token_type;
} rules[] = {

    /* TODO: Add more rules.
     * Pay attention to the precedence level of different rules.
     */

    {" +", TK_NOTYPE}, // spaces
    {"\\+", '+'},      // plus
    {"\\=\\=", TK_EQ}, // equal
    {"\\-", '-'},      // sub
    {"\\*", '*'},      // mul
    {"\\/", '/'},      // div
    {"\\(", '('},      // left bracket
    {"\\)", ')'},      // right bracket

    {"\\!\\=", TK_UEQ},             // unequal
    {"&&", TK_AND},                 // and
    {"0[xX][0-9A-Fa-f]*", TK_HEX},  // hex
    {"\\$[a-zA-Z]*[0-9]*", TK_REG}, // register
    {"[0-9]*", TK_NUM},             // number
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex()
{
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i++)
  {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0)
    {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token
{
  int type;
  char str[32];
} Token;

static Token tokens[65536] __attribute__((used)) = {};
static int nr_token __attribute__((used)) = 0;

bool check_parentheses(int p, int q);
int32_t eval(int p, int q);

bool check_parentheses(int p, int q);
int32_t eval(int p, int q);

static bool make_token(char *e)
{
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0')
  {
    /* Try all rules one by one. */
    // Log("%d", NR_REGEX);
    for (i = 0; i < NR_REGEX; i++)
    {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 &&
          pmatch.rm_so == 0)
      {

        int substr_len = pmatch.rm_eo;
        // char *substr_start = e + position;
        // Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s", i,
        //   rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        switch (rules[i].token_type)
        {
        case TK_NOTYPE:
          break;
        case TK_NUM:
        case TK_REG:
        case TK_HEX:
          tokens[nr_token].type = rules[i].token_type;
          strncpy(tokens[nr_token].str, &e[position - substr_len], substr_len);
          nr_token++;
          break;
        case '+':
        case '-':
        case '*':
        case '/':
        case '(':
        case ')':
        case TK_EQ:
        case TK_UEQ:
        case TK_AND:
          tokens[nr_token++].type = rules[i].token_type;
          break;
        default:
          Assert(0, "Unrecognized token type");
        }

        break;
      }
    }

    if (i == NR_REGEX)
    {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

word_t expr(char *e, bool *success)
{
  if (!make_token(e))
  {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  /* */
  int flag = 0;
  for (int i = 0; i < nr_token; i++)
  {
    if (tokens[i].type == '-' && (i == 0 || ((tokens[i - 1].type != TK_NUM) &&
                                             (tokens[i - 1].type != ')'))))
    {
      flag++;
      char resultStr[32] = "-";
      strcat(resultStr, tokens[i + 1].str);
      strcpy(tokens[i + 1].str, resultStr);

      for (int j = i; j < nr_token; j++)
      {
        memset(tokens[j].str, '\0', sizeof(tokens[j].str) / sizeof(char));
        tokens[j].type = tokens[j + 1].type;
        strcpy(tokens[j].str, tokens[j + 1].str);
      }
      memset(tokens[nr_token - 1].str, '\0',
             sizeof(tokens[nr_token - 1].str) / sizeof(char));
      i++;
    }

    if (tokens[i].type == '*' && (i == 0 || (tokens[i - 1].type != TK_NUM &&
                                             tokens[i - 1].type != ')')))
    {
      flag++;

      for (int j = i; j < nr_token; j++)
      {
        memset(tokens[j].str, '\0', sizeof(tokens[j].str) / sizeof(char));
        tokens[j].type = tokens[j + 1].type;
        strcpy(tokens[j].str, tokens[j + 1].str);
      }
      memset(tokens[nr_token - 1].str, '\0',
             sizeof(tokens[nr_token - 1].str) / sizeof(char));
      tokens[i].type = TK_POINT;
      i++;
    }
  }
  return eval(0, nr_token - 1 - flag);
}

int32_t eval(int p, int q)
{
  if (p > q)
  {
    /* Bad expression */
    Assert(0, "bad expression!!");
    return -1;
  }
  else if (p == q)
  {
    /* Single token.
     * For now this token should be a number.
     * Return the value of the number.
     */
    if (tokens[p].type == TK_NUM)
    {
      word_t num = atoi(tokens[p].str);
      memset(tokens[p].str, '\0', sizeof(tokens[p].str) / sizeof(char));
      return num;
    }
    else if (tokens[p].type == TK_REG)
    {
      bool success;
      word_t result = isa_reg_str2val(&tokens[p].str[1], &success);
      if (success)
      {
        memset(tokens[p].str, '\0', sizeof(tokens[p].str) / sizeof(char));
        return result;
      }
      else
      {
        memset(tokens[p].str, '\0', sizeof(tokens[p].str) / sizeof(char));
        Log("bad expression!!");
        return -1;
      }
    }
    else if (tokens[p].type == TK_HEX)
    {
      word_t num = (word_t)strtoul(tokens[p].str, NULL, 16);
      memset(tokens[p].str, '\0', sizeof(tokens[p].str) / sizeof(char));
      // Log("num:0x%08x type:%d", num, tokens[p].type);
      return num;
    }
    else if (tokens[p].type == TK_POINT)
    {
      word_t num = (word_t)strtoul(tokens[p].str, NULL, 16);
      memset(tokens[p].str, '\0', sizeof(tokens[p].str) / sizeof(char));

      num = vaddr_read(num, 4);
      return num;
    }

    return 0;
  }
  else if (check_parentheses(p, q) == true)
  {
    return eval(p + 1, q - 1);
  }
  else
  {
    int op = p;
    // puts("ss");
    int count = 0;
    bool flag_2 = false;
    bool flag_1 = false;
    bool flag_3 = false;
    for (int i = p; i <= q; i++)
    { // 运算符优先级
      switch (tokens[i].type)
      {
      case '(':
        count++;
        break;
      case ')':
        count--;
        break;
      case '+':
      case '-':
        if (count == 0 && flag_2 == false && flag_3 == false)
        {
          op = i;
          flag_1 = true;
        }
        break;

      case '*':
      case '/':
        if (flag_1 == false && count == 0 && flag_2 == false && flag_3 == false)
          op = i;
        break;

      case TK_EQ:
      case TK_UEQ:
        if (count == 0)
        {
          op = i;
          flag_2 = true;
        }
        break;
      case TK_AND:
        if (count == 0)
        {
          op = i;
          flag_3 = true;
        }
      default:
        break;
      }
    }

    int32_t val1 = eval(p, op - 1);
    int32_t val2 = eval(op + 1, q);
    char op_type = tokens[op].type;

    switch (op_type)
    { // 运算符计算
    case '+':
      return val1 + val2;
    case '-':
      return val1 - val2;
    case '*':
      return val1 * val2;
    case '/':
      if (val2 == 0)
      {
        Assert(0, "divid zero!!");
      }
      else
        return val1 / val2;
    case TK_EQ:
      return val1 == val2;
    case TK_UEQ:
      return val1 != val2;
    case TK_AND:
      return val1 && val2;
    default:
      Assert(0, "bad expression!!");
    }
  }
}

bool check_parentheses(int p, int q)
{
  int i;
  int count = 0;
  if (tokens[p].type != '(' || tokens[q].type != ')')
  {
    // Assert(0, "bad expression!!");
    return false;
  }

  for (i = p + 1; i < q; i++)
  {
    if (tokens[i].type == '(')
      count++;
    else if (tokens[i].type == ')')
      count--;
    if (count < 0)
      return false;
  }
  if (count == 0)
    return true;
  else
  {
    Assert(0, "bad expression!!");
    return false;
  }
}
