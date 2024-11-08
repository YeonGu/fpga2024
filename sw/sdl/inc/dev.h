#ifndef __DEV_H__
#define __DEV_H__

#define CTRLREG_BASE 0x3EF40000
#define VRAM_BASE 0xB0100000

#define MVP_OFF 0x0
#define BASE_COORD_OFF 0x30
#define START_OFF 0x40
#define CALC_CNT_OFF 0x50
#define DISPATCH_CNT_OFF 0x60

#define REG(x) (ctrlreg_vptr + x##_##OFF / sizeof(uint32_t))
#define TEXTURE_RAM_SIZE (512 * 512 * 512)

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>
#include <Eigen/Dense>

struct Dev {
    int fd;
    uint32_t *ctrlreg_vptr;
    uint8_t *texture_ram_vptr;
    uint8_t *vram_vptr;

    Dev();
    void start_render();
    void write_mvp_matrix(Eigen::Matrix4i &mvp);
    void write_base_coord();
    void load_texture(const char *filename);
};

#endif // __DEV_H__
