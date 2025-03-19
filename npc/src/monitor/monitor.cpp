#include <getopt.h>
#include <common.h>
#include <memory/paddr.h>

static int is_batch_mode = false;

static char *log_file = NULL;
static char *diff_so_file = NULL;
static char *img_file = NULL;
static int difftest_port = 1234;

void init_rand();
void init_log(const char *log_file);
void init_mem();
void init_isa();
void init_sdb();
void sdb_set_batch_mode();

static int parse_args(int argc, char *argv[]);
static long load_img();

static void welcome()
{
    Log("Trace: %s", MUXDEF(CONFIG_TRACE, ANSI_FMT("ON", ANSI_FG_GREEN), ANSI_FMT("OFF", ANSI_FG_RED)));
    IFDEF(CONFIG_TRACE, Log("If trace is enabled, a log file will be generated "
                            "to record the trace. This may lead to a large log file. "
                            "If it is not necessary, you can disable it in menuconfig"));
    Log("Build time: %s, %s", __TIME__, __DATE__);
    printf("Welcome to %s-NEMU!\n", ANSI_FMT(str(__GUEST_ISA__), ANSI_FG_YELLOW ANSI_BG_RED));
    printf("For help, type \"help\"\n");
}
void init_monitor(int argc, char *argv[])
{ //$(ARGS) $(IMG)
    parse_args(argc, argv);
    init_rand();
    init_log(log_file);
    init_mem();
    init_isa();
    long img_size = load_img();
    // init_difftest(diff_so_file, img_size, difftest_port);
    init_sdb();
    welcome();
}

static long load_img()
{
    if (img_file == NULL)
    {
        Log("No image is given. Use the default build-in image.");
        return 4096; // built-in image size
    }

    FILE *fp = fopen(img_file, "rb");
    Assert(fp, "Can not open '%s'", img_file);

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);

    Log("The image is %s, size = %ld", img_file, size);

    fseek(fp, 0, SEEK_SET);
    int ret = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
    assert(ret == 1);

    fclose(fp);
    return size;
}
static int parse_args(int argc, char *argv[])
{
    /*
    struct option：用于定义长选项的结构体。
    第一个字段是长选项的名称（如 "batch"）。
    第二个字段指定该选项是否需要参数：
    no_argument：不需要参数。
    required_argument：需要参数。
    第三个字段是一个指针，用于存储选项的状态（这里未使用，设置为 NULL）。
    第四个字段是对应的短选项字符（如 'b'）。
    */
    const struct option table[] = {
        {"batch", no_argument, NULL, 'b'},
        {"log", required_argument, NULL, 'l'},
        {"diff", required_argument, NULL, 'd'},
        {"port", required_argument, NULL, 'p'},
        {"help", no_argument, NULL, 'h'},
        {0, 0, NULL, 0},
    };
    int o;
    /*
    getopt_long：用于解析命令行参数，支持长选项和短选项。
    argc 和 argv 是命令行参数。
    "-bhl:d:p:" 是短选项字符串，表示支持的短选项：
    -b：无参数。
    -h：无参数。
    -l：需要参数。
    -d：需要参数。
    -p：需要参数。
    table 是长选项表。
    NULL：用于存储当前解析的长选项的索引（这里未使用）。*/
    while ((o = getopt_long(argc, argv, "-bhl:d:p:", table, NULL)) != -1)
    {
        switch (o)
        {
        case 'b':
            sdb_set_batch_mode();
            break;
        case 'p':
            sscanf(optarg, "%d", &difftest_port);
            break;
        case 'l':
            log_file = optarg;
            break;
        case 'd':
            diff_so_file = optarg;
            break;
        case 1:
            img_file = optarg;
            return 0;
        default:
            printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
            printf("\t-b,--batch              run with batch mode\n");
            printf("\t-l,--log=FILE           output log to FILE\n");
            printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
            printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
            printf("\n");
            exit(0);
        }
    }
    return 0;
}