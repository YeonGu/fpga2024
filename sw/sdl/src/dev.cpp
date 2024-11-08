#include "dev.h"
#include <cstddef>
#include <cstdio>
#include <unistd.h>

Dev::Dev() {
    fd = open("/dev/mem", O_RDWR | O_SYNC);
    if (fd < 0) {
        fprintf(stderr, "Cannot open /dev/mem\n");
        return;
    }

    ctrlreg_vptr = (uint32_t *)mmap(NULL, 0x1000, PROT_READ | PROT_WRITE,
                                    MAP_SHARED, fd, CTRLREG_BASE);
    if (ctrlreg_vptr == MAP_FAILED) {
        fprintf(stderr, "Cannot mmap ctrlreg\n");
        return;
    }

    texture_ram_vptr =
        (uint8_t *)mmap(NULL, TEXTURE_RAM_SIZE, PROT_READ | PROT_WRITE,
                        MAP_SHARED, fd, VRAM_BASE);
    if (texture_ram_vptr == MAP_FAILED) {
        fprintf(stderr, "Cannot mmap texture ram\n");
        return;
    }

    vram_vptr =
        (uint8_t *)mmap(NULL, 960 * 720, PROT_READ, MAP_SHARED, fd, VRAM_BASE);
    if (vram_vptr == MAP_FAILED) {
        fprintf(stderr, "Cannot mmap vram\n");
        return;
    }

    close(fd);
}

void Dev::start_render() {
    volatile uint32_t *start_reg = REG(START);
    *start_reg = 0x01;
}

void Dev::write_mvp_matrix(Eigen::Matrix4i &mvp) {
    volatile uint32_t *mvp_reg = REG(MVP);
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            mvp_reg[i * 4 + j] = mvp(i, j);
        }
    }
}

void Dev::write_base_coord() {
    volatile uint32_t *base_coord_reg = REG(BASE_COORD);
    base_coord_reg[0] = -32;
    base_coord_reg[1] = -32;
    base_coord_reg[2] = -32;
}

void Dev::load_texture(const char *filename) {
    FILE *fp = fopen(filename, "rb");
    if (!fp) {
        fprintf(stderr, "Cannot open file %s\n", filename);
        exit(1);
    }
    fread(texture_ram_vptr, sizeof(std::byte), 512 * 512 * 512, fp);
    fclose(fp);
}