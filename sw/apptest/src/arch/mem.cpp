#include <arch.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

uint8_t* Mem::get_vram_vptr()
{
    static uint8_t* vram_vptr  = nullptr;
    constexpr int   vram_depth = 960 * 720;
    if(vram_vptr == nullptr) {
        vram_vptr = (uint8_t*)mmap(NULL, vram_depth, PROT_READ, MAP_SHARED, fd, VRAM_BASE);
    }

    return vram_vptr;
}

Mem::Mem()
{
    fd = open("/dev/mem", O_RDWR | O_SYNC);
    if(fd < 0) {
        printf("Error: cannot open /dev/mem\n");
        exit(1);
    }
    ctrlreg_vptr
        = (uint32_t*)mmap(NULL, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED, fd, CTRLREG_BASE);
}

Mem::~Mem() { close(fd); }