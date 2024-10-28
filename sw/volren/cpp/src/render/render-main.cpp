#include "config.h"
// #include <Eigen/Dense>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <eigen3/Eigen/Dense>
#include <image.h>
#include <iostream>
#include <memory>
#include <opencv2/core/hal/interface.h>
#include <opencv2/highgui.hpp>
#include <opencv2/opencv.hpp>
#include <vector>

/* Coordinate System:
 *
 * Y|
 *  |   /Z-
 *  |  /
 *  | /
 *  |/________X
 *
 */

Eigen::Matrix4f camera_matrix();

Eigen::Vector4f raster2world(uint32_t raster_x, uint32_t raster_y,
                             const resolution_t& target_resolution, const Eigen::Matrix4f& cam_mat);

void render_entry()
{
    // load image stack
    auto image_stack = load_ct_image(1006, 250);
    auto cam_mat     = camera_matrix();

    std::cout << "cam_mat: " << cam_mat << std::endl;

    Eigen::Vector4f        cam_pos{CAM_POS_X, CAM_POS_Y, CAM_POS_Z, 1};
    constexpr resolution_t target_resolution = {512, 512};

    struct samples_t
    {
        uint16_t color;
        float    opacity;
    };

    // std::array<std::array<uint16_t, target_resolution.col>, target_resolution.row> render_image;
    static uint8_t render_image[target_resolution.row][target_resolution.col];

    /* ray tracing */
    for(uint32_t i = 0; i < target_resolution.row; i++) {
        printf("Rendering row %d\n", i);
        for(uint32_t j = 0; j < target_resolution.col; j++) {
            auto            pix_world = raster2world(i, j, target_resolution, cam_mat);
            Eigen::Vector4f eye_ray   = pix_world - cam_pos;
            eye_ray.normalize();

            // std::cout << "screen:" << i << j << "eye_ray: " << eye_ray << std::endl;

            // carry out ray tracing
            uint32_t trace_times = MAX_TRACE_DISTANCE / RAYTRACE_STEP;
            auto     trace_pos   = cam_pos;

            std::vector<samples_t> samples = {};

            // TODO: use oct-tree to accelerate ray tracing
            while(trace_times--) {
                trace_pos += eye_ray * RAYTRACE_STEP;
                float opacity      = image_stack->sample_opacity_world(trace_pos);
                float cam_distance = (trace_pos - cam_pos).norm();
                // std::cout << "trace pos: \n" << trace_pos << std::endl;
                // if(i == target_resolution.row / 2 && j == target_resolution.col / 2)
                //     printf("trace pos: %f %f %f\n", trace_pos[0], trace_pos[1], trace_pos[2]);
                if(opacity > 0.0f) {
                    samples.push_back(
                        {image_stack->sample_color_world(trace_pos, cam_distance), opacity});
                    // std::cout << "sample color: " << samples.back().color
                    //           << "\n sample opacity: " << samples.back().opacity << std::endl;
                }
                if(opacity > 0.95f) {
                    break;
                }
            }

            // composite samples
            // see paper "Volume Rendering" by Marc Levoy for more details
            uint8_t composite_color = 0;
            for(auto it = samples.rbegin(); it != samples.rend(); ++it) {
                composite_color = composite_color * (1 - it->opacity) + it->color * it->opacity;
            }
            // if(i == 27 && j == 30)
            //     printf("composite_color: %d\n", composite_color);
            // if(i == 28 && j == 30)
            //     printf("composite_color: %d\n", composite_color);
            render_image[i][j] = composite_color;
        }
    }

    // for(uint32_t i = 0; i < target_resolution.row; i++) {
    //     printf("%d:%d  ", i, render_image[i][30]);
    // }
    // printf("\n%d %d\n", render_image[27][30], render_image[28][30]);

    // display render image
    cv::Mat img_mat(target_resolution.row, target_resolution.col, CV_8U, render_image);
    cv::imshow("Render Image", img_mat);
    while(cv::waitKey(0) != 27) {
        // Press 'Esc' key to exit
    }
}

// std::shared_ptr<image_t> ray_tracing() { }
