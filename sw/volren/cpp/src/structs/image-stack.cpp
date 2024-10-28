#include "config.h"
#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <image.h>
#include <opencv2/core/hal/interface.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/opencv.hpp>
#include <stdexcept>
#include <vector>

ImageStack::ImageStack() : width(0), slices(0) { }

void ImageStack::add_image(const image_t& img)
{
    if(images.empty()) {
        width  = img.size();
        slices = 1;
    }
    else {
        if(img.size() != width)
            throw std::runtime_error("Image width mismatch");
        slices++;
    }
    images.push_back(img);
}

void ImageStack::display(uint32_t slice_index) const
{
    if(slice_index >= slices)
        throw std::runtime_error("Invalid slice index");

    // cv::Mat img_mat(width, width, CV_8UC1, const_cast<uint32_t*>(images[slice_index].data()));
    auto img_mat = new uint16_t[width * width];
    for(uint32_t i = 0; i < width; i++) {
        for(uint32_t j = 0; j < width; j++) {
            img_mat[i * width + j] = static_cast<uint16_t>(images[slice_index][i][j] * 32);
        }
    }
    cv::Mat img_mat_cv(width, width, CV_16U, img_mat);
    cv::imshow("Image Slice", img_mat_cv);
    cv::waitKey(0);
    delete[] img_mat;
}

float ImageStack::y_abs_boundary() const
{
    return (static_cast<float>(slices) / width) * (SLICE_SIZE / PIXEL_SIZE);
}

// void ImageStack::prepare_gradients()
// {
//     gradients.resize(slices, width, width);
//     for(uint32_t i = 0; i < slices; i++) {
//         for(uint32_t j = 0; j < width; j++) {
//             for(uint32_t k = 0; k < width; k++) {
//                 Eigen::Vector4f grad = gradient(Eigen::Vector4f(j, k, i, 1));
//                 gradients(i, j, k)   = grad;
//             }
//         }
//     }
// }

Eigen::Vector4f ImageStack::gradient(Eigen::Vector4f& world_pos) const
{
    Eigen::Vector3f voxel_pos = world2voxel(world_pos);

    {
        uint32_t vx = static_cast<uint32_t>(voxel_pos[0]);
        uint32_t vy = static_cast<uint32_t>(voxel_pos[1]);
        uint32_t vz = static_cast<uint32_t>(voxel_pos[2]);

        if(vx == 0 || vx == width - 1 || vy == 0 || vy == width - 1 || vz == 0
           || vz == slices - 1) {
            return Eigen::Vector4f(0, 0, 0, 0);
        }
    }

    float vx = voxel_pos[0];
    float vy = voxel_pos[1];
    float vz = voxel_pos[2];

    float vox_dx = (sample_voxel(vx + 1, vy, vz) - sample_voxel(vx - 1, vy, vz)) / 2.0f;
    float vox_dy = (sample_voxel(vx, vy + 1, vz) - sample_voxel(vx, vy - 1, vz)) / 2.0f;
    float vox_dz = (sample_voxel(vx, vy, vz + 1) - sample_voxel(vx, vy, vz - 1)) / 2.0f;

    vox_dx *= PIXEL_SIZE / SLICE_SIZE;
    // float vox_dx = (images[vx + 1][vy][vz] - images[vx - 1][vy][vz]) / 2.0f;
    // float vox_dy = (images[vx][vy + 1][vz] - images[vx][vy - 1][vz]) / 2.0f;
    // float vox_dz = (images[vx][vy][vz + 1] - images[vx][vy][vz - 1]) / 2.0f;

    // change coordinate system to fit the world coordinate system
    return Eigen::Vector4f(vox_dz, -vox_dx, -vox_dy, 0);
}

uint16_t ImageStack::sample_voxel(float x, float y, float z) const
{
    if(x <= 1 || x >= slices - 2 || y <= 1 || y >= width - 2 || z <= 1 || z >= width - 2)
        return 0;

    // trilinear interpolation
    int x0 = static_cast<int>(x);
    int y0 = static_cast<int>(y);
    int z0 = static_cast<int>(z);
    int x1 = x0 + 1;
    int y1 = y0 + 1;
    int z1 = z0 + 1;

    float xd = x - x0;
    float yd = y - y0;
    float zd = z - z0;

    float c00 = images[x0][y0][z0] * (1 - xd) + images[x1][y0][z0] * xd;
    float c01 = images[x0][y1][z0] * (1 - xd) + images[x1][y1][z0] * xd;
    float c10 = images[x0][y0][z1] * (1 - xd) + images[x1][y0][z1] * xd;
    float c11 = images[x0][y1][z1] * (1 - xd) + images[x1][y1][z1] * xd;

    float c0 = c00 * (1 - yd) + c01 * yd;
    float c1 = c10 * (1 - yd) + c11 * yd;

    return static_cast<uint16_t>((c0 * (1 - zd) + c1 * zd));
}

uint16_t ImageStack::sample_color_world(Eigen::Vector4f& world_pos, float cam_distance) const
{
    if(!is_inside_world(world_pos))
        return 0;

    // Eigen::Vector3f voxel_pos = world2voxel(world_pos);
    // Compute shading
    // float sample                 = sample_voxel(voxel_pos[0], voxel_pos[1], voxel_pos[2]);
    float ambient_ref_coeff      = 170.0f; // environment ambient light
    float diffuse_ref_coeff      = 200.0f; // diffuse reflection
    float light_source_intensity = 1.0f;
    float depth_cue_k1           = 1.0f;
    float depth_cue_k2           = 0.8f;

    const Eigen::Vector3f light_dir(0, -1, 0);
    const Eigen::Vector3f light_norm = light_dir.normalized();

    // Eigen::Vector4f normal = gradient(world_pos);
    // normal.head<3>()       = normal.head<3>().normalized();
    auto normal = this->normal(world_pos);

    float distance  = cam_distance;
    float shade_res = light_source_intensity * ambient_ref_coeff
                      + (light_source_intensity / (depth_cue_k1 + depth_cue_k2 * distance))
                            * (diffuse_ref_coeff * (light_norm.dot(normal.head<3>())));

    return static_cast<uint16_t>(shade_res);
}

float ImageStack::sample_opacity_world(Eigen::Vector4f& world_pos) const
{
    if(!is_inside_world(world_pos))
        return 0.0f;

    Eigen::Vector3f voxel_pos = world2voxel(world_pos);
    // std::cout << world_pos << "\n->\n" << voxel_pos << std::endl;

    float vox_sample = sample_voxel(voxel_pos[0], voxel_pos[1], voxel_pos[2]);
    // auto y_boundary = y_abs_boundary();

    // clang-format off
#ifdef THRES_SKIN
    struct val_levels
    {
        float tissue, opacity;
    } levels[] = {{20, 0.0f},   
                {50, 0.0f},   
                {100, 0.0f},  
                {500, 0.0f}, 
                {900, 1.0f},
                {1400, 1.0f}, 
                {1800, 0.0f}, 
                {2500, 0.0f}, 
                {3000, 0.0f}};
    // clang-format on
#endif
#ifdef THRES_BONE
    // clang-format off
    struct val_levels
    {
        float tissue, opacity;
    } levels[] = {{20, 0.0f},   
                {50, 0.0f},   
                {100, 0.0f},  
                {150, 0.0f}, 
                {200, 0.0f},
                {1000, 0.0f}, 
                {1500, 0.0f},
                {2000, 1.0f}, 
                {2500, 1.0f}, 
                {3000, 1.0f}};
    // clang-format on
#endif

    float interpolation = 0.0f;
    // float gradient_thres = 3000.0f;

    // auto clamp = [](float val, float thres) -> float {
    //     return val < thres ? val : thres;
    // };
    auto get_tissue_opacity = [=](float sample) -> float {
        for(size_t i = 0; i < sizeof(levels) / sizeof(levels[0]) - 1; ++i) {
            if(sample >= levels[i].tissue && sample < levels[i + 1].tissue) {
                float res = levels[i].opacity
                            + (levels[i + 1].opacity - levels[i].opacity)
                                  * (sample - levels[i].tissue)
                                  / (levels[i + 1].tissue - levels[i].tissue);
                // printf("sample %f,%f,%f -> %f -> %f\n", world_pos[0], world_pos[1], world_pos[2],
                //        sample, res);
                return res;
            }
        }
        return 0.0f;
    };

    for(size_t i = 0; i < sizeof(levels) / sizeof(levels[0]) - 1; ++i) {
        if(vox_sample >= levels[i].tissue && vox_sample < levels[i + 1].tissue) {
            interpolation = get_tissue_opacity(vox_sample);
        }
    }

    Eigen::Vector3f voxel_gradient     = gradient(world_pos).head<3>();
    float           gradient_magnitude = voxel_gradient.norm();

    auto thres_normalize = [](float val, float thres) -> float {
        return val < thres ? val / thres : 1.0f;
    };

    // if(gradient_magnitude != 0 && interpolation != 0) {
    //     std::cout << "\ngradient magnitude: " << gradient_magnitude << std::endl;
    //     std::cout << "interpolation: " << interpolation << std::endl;
    // }

    return interpolation * thres_normalize(gradient_magnitude, 100.0f);
}
// {
//     Eigen::Vector3i voxel_pos = world2voxel(world_pos);
//     if(voxel_pos[0] == 0 || voxel_pos[0] == width - 1 || voxel_pos[1] == 0
//        || voxel_pos[1] == width - 1 || voxel_pos[2] == 0 || voxel_pos[2] == slices - 1) {
//         return Eigen::Vector4f(0, 0, 0, 0);
//     }

//     float dx = (sample_voxel_world(voxel_pos[0] + 1, voxel_pos[1], voxel_pos[2])
//                 - sample_voxel_world(voxel_pos[0] - 1, voxel_pos[1], voxel_pos[2]))
//                / 2;
//     float dy = (sample_voxel_world(voxel_pos[0], voxel_pos[1] + 1, voxel_pos[2])
//                 - sample_voxel_world(voxel_pos[0], voxel_pos[1] - 1, voxel_pos[2]))
//                / 2;
//     float dz = (sample_voxel_world(voxel_pos[0], voxel_pos[1], voxel_pos[2] + 1)
//                 - sample_voxel_world(voxel_pos[0], voxel_pos[1], voxel_pos[2] - 1))
//                / 2;

//     return Eigen::Vector4f(dx, dy, dz, 0);
// }

bool ImageStack::is_inside_world(Eigen::Vector4f world_pos) const
{
    float y_boundary = y_abs_boundary();
    return abs(world_pos[0]) < 1 && abs(world_pos[1]) < y_boundary && abs(world_pos[2]) < 1;
}

Eigen::Vector3f ImageStack::world2voxel(Eigen::Vector4f& world_pos) const
{
    assert(is_inside_world(world_pos));
    float vox_xmap = (-world_pos[1] + y_abs_boundary()) * (slices) / (2 * y_abs_boundary());
    float vox_ymap = (-world_pos[2] + 1) * (width) / 2;
    float vox_zmap = (world_pos[0] + 1) * (width) / 2;

    return Eigen::Vector3f(vox_xmap, vox_ymap, vox_zmap);
}