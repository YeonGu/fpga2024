#ifndef __ARCH_H__
#define __ARCH_H__

#include <cstdint>
#include <cstdio>

#define CTRLREG_BASE 0xB0000000
#define VRAM_BASE 0xB0100000

#define MVP_OFF 0x0
#define BASE_COORD_OFF 0x30
#define START_OFF 0x40
#define CALC_CNT_OFF 0x50
#define DISPATCH_CNT_OFF 0x60

class Mem {
private:
    int       fd;
    uint32_t* ctrlreg_vptr;

public:
    Mem();
    ~Mem();

    uint8_t* get_vram_vptr();

public:
    // control register io
    void render_start();
    void write_mvp();
    void write_basecoord();

public:
    uint32_t get_dispatch_cnt();
    uint32_t get_render_cnt(int id);

public:
    void load_texture();
};

#endif