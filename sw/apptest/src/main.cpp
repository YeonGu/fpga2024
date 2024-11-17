#include "info.h"
#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <eigen3/Eigen/Dense>
#include <iostream>
#include <unistd.h>

#define ZOOM_

int main(int argc, char** argv)
{
    // if(argc != 2) {
    //     printf("Usage: %s <texture_path>\n", argv[0]);
    //     exit(1);
    // }

    camera_transform_t trans = {.position    = Eigen::Vector3f(0.0f, 0.0f, 0.0f),
                                .yaw         = 0.1f,
                                .pitch       = 0.1f,
                                .ortho_scale = 3.5f};

    Eigen::Matrix4f mvp = gen_mvp_matrix(trans);
    std::cout << mvp << std::endl;

    auto vram = (uint8_t*)malloc(sizeof(std::byte) * 960 * 720);

    static uint8_t texture_mem[512][512][512];
    // static uint8_t vram[960][720];
    FILE* fp = fopen("../volume.raw", "rb");
    if(!fp) {
        printf("Error: cannot open volume.raw\n");
        exit(1);
    }
    fread(texture_mem, sizeof(uint8_t), 512 * 512 * 512, fp);
    fclose(fp);

    auto norm = [](float x) -> float {
        return (x - 256.0f) / 256.0f;
    };
    auto norm2screen = [](float x, float y) -> std::pair<int, int> {
        uint32_t x_ = static_cast<uint32_t>(x * 480 + 480);
        uint32_t y_ = static_cast<uint32_t>(y * 360 + 360);
        if(x_ >= 960) {
            x_ = 959;
        }
        if(y_ >= 720) {
            y_ = 719;
        }
        return std::make_pair(x_, y_);
    };

    const unsigned int frames = 150;

    // yaw Rotate
    for(unsigned int frame = 0; frame < frames; frame++) {
#ifdef ROTATE_
        trans.yaw += 0.05f;
#endif
#ifdef ZOOM_
        trans.ortho_scale -= 0.009f;
#endif

        mvp = gen_mvp_matrix(trans);
        // memset(vram, 0, 960 * 720);
        printf("rendering frame %d\n", frame);

        memset(vram, 0, 960 * 720);

        for(int i = 0; i < 512; i++) {
            for(int j = 0; j < 512; j++) {
                for(int k = 20; k < 500; k++) {
                    auto val = texture_mem[i][j][k];

                    Eigen::Vector4f coord = Eigen::Vector4f(norm(j), norm(i), norm(k), 1.0f);

                    coord = mvp * coord;
                    // coord to screen space
                    if(std::abs(coord(1)) > 1.0f || std::abs(coord(0)) > 1.0f) {
                        continue;
                    }

                    auto original       = norm2screen(coord(0), coord(1));
                    auto original_color = vram[original.second * 960 + original.first];
                    if(original_color < val) {
                        vram[original.second * 960 + original.first] = val;
                    }
                }
            }
        }

        char filename[32];
#ifdef ROTATE_
        sprintf(filename, "./out/%03d.ppm", frame);
#endif
#ifdef ZOOM_
        sprintf(filename, "./zoomout/%03d.ppm", frame);
#endif
        FILE* fp1 = fopen(filename, "wb");
        if(!fp1) {
            printf("Error: cannot open %s\n", filename);
            exit(1);
        }
        fprintf(fp1, "P5\n960 720\n255\n");
        fwrite(vram, sizeof(std::byte), 960 * 720, fp1);
        fclose(fp1);
    }

    free(vram);
    return 0;
}