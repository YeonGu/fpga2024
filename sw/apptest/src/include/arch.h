#ifndef __ARCH_H__
#define __ARCH_H__

#include <Eigen/Dense>
#include <cstdint>
#include <cstdio>

#define CTRLREG_BASE 0xB0000000
#define VRAM_BASE 0xB0100000

#define MVP_OFF 0x0
#define BASE_COORD_OFF 0x30
#define START_OFF 0x40
#define CALC_CNT_OFF 0x50
#define DISPATCH_CNT_OFF 0x60

#define REG(x) (ctrlreg_vptr + x##_##OFF / sizeof(uint32_t))
#define TEXTURE_RAM_SIZE (512 * 512 * 512)

class Mem {
private:
    int       fd;
    uint32_t* ctrlreg_vptr;
    uint8_t*  texture_ram_vptr; // mapped to 0x0000_0000 in DDR

public:
    Mem();
    ~Mem();

    uint8_t* get_vram_vptr();

public:
    // TODO: texture ram load

public:
    // control register io
    void render_start();
    void write_mvp(Eigen::Matrix4i& mvp);
    void write_basecoord();

public:
    uint32_t get_dispatch_cnt();
    uint32_t get_render_cnt(int id);

public:
    void load_texture(const char* pathname);
};

#endif