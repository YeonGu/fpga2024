#include <arch.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

Mem::Mem()
{
    fd = open("/dev/mem", O_RDWR | O_SYNC);
    if(fd < 0) {
        printf("Error: cannot open /dev/mem\n");
        exit(1);
    }
    ctrlreg_vptr
        = (uint32_t*)mmap(NULL, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED, fd, CTRLREG_BASE);
    texture_ram_vptr
        = (uint8_t*)mmap(NULL, TEXTURE_RAM_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0x0);
}

Mem::~Mem() { close(fd); }

uint8_t* Mem::get_vram_vptr()
{
    static uint8_t* vram_vptr  = nullptr;
    constexpr int   vram_depth = 960 * 720;
    if(vram_vptr == nullptr) {
        vram_vptr = (uint8_t*)mmap(NULL, vram_depth, PROT_READ, MAP_SHARED, fd, VRAM_BASE);
    }

    return vram_vptr;
}

void Mem::render_start()
{
    volatile uint32_t* start_reg = REG(START);
    *start_reg                   = 0x01;
}
void Mem::write_mvp(Eigen::Matrix4i& mvp)
{
    volatile uint32_t* mvp_reg = REG(MVP);
    // TODO:
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            mvp_reg[i * 4 + j] = mvp(i, j);
        }
    }
}
void Mem::write_basecoord()
{
    volatile uint32_t* basecoord_reg = REG(BASE_COORD);
    // TODO:
    basecoord_reg[0] = -32;
    basecoord_reg[1] = -32;
    basecoord_reg[2] = -32;
}

void Mem::load_texture(const char* pathname)
{
    FILE* fp = fopen(pathname, "rb");
    if(!fp) {
        printf("Error: cannot open texture file\n");
        exit(1);
    }
    fread(texture_ram_vptr, 8, 512 * 512 * 512, fp);
    fclose(fp);
}