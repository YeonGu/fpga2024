#include "arch.h"
#include "info.h"
#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <cstring>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("Usage: %s <texture_path>\n", argv[0]);
        exit(1);
    }
    Mem                mem;
    camera_transform_t trans;
    memset(&trans, 0, sizeof(camera_transform_t));
    Eigen::Matrix4i mvp = gen_mvp_matrix(trans);
    mem.write_mvp(mvp);
    mem.load_texture(argv[1]);
    mem.write_basecoord();
    mem.render_start();
    uint8_t* vram = mem.get_vram_vptr();
    usleep(100);
    uint8_t *vram_buffer = (uint8_t *)malloc(sizeof(std::byte) * 960 * 720);
    memcpy(vram_buffer, vram, sizeof(std::byte) * 960 * 720);
    FILE *fp = fopen("./output.ppm", "rb");
    if (!fp) {
        printf("Error: cannot open output.ppm\n");
        exit(1);
    }
    fprintf(fp, "P5\n960 720\n255\n");
    fwrite(vram_buffer, sizeof(std::byte), 960 * 720, fp);
    fclose(fp);
    free(vram_buffer);
    return 0;
}