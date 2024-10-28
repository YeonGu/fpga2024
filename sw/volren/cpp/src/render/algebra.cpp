
// #include "Eigen/src/Core/Matrix.h"
#include "config.h"
#include <Eigen/Dense>
#include <cmath>
// #include <iostream>

/*
cam_mat = (
    np.array(
        # POSITION
        [
            [1, 0, 0, cam_pos[0, 0]],
            [0, 1, 0, cam_pos[1, 0]],
            [0, 0, 1, cam_pos[2, 0]],
            [0, 0, 0, 1],
        ]
    )
    @ np.array(
        # YAW
        [
            [np.cos(yaw_rad), 0, -np.sin(yaw_rad), 0],
            [0, 1, 0, 0],
            [np.sin(yaw_rad), 0, np.cos(yaw_rad), 0],
            [0, 0, 0, 1],
        ]
    )
    @ np.array(
        # PITCH
        [
            [1, 0, 0, 0],
            [0, np.cos(pitch_rad), -np.sin(pitch_rad), 0],
            [0, np.sin(pitch_rad), np.cos(pitch_rad), 0],
            [0, 0, 0, 1],
        ]
    )
)
*/
Eigen::Matrix4f camera_matrix()
{
    Eigen::Vector3f cam_pos(CAM_POS_X, CAM_POS_Y, CAM_POS_Z); // Example camera position

    auto deg2rad = [](float deg) {
        return deg * M_PI / 180;
    };
    float yaw_rad   = deg2rad(CAM_YAW);   // Example yaw angle in radians
    float pitch_rad = deg2rad(CAM_PITCH); // Example pitch angle in radians

    // clang-format off
    Eigen::Matrix4f pos_mat;
    pos_mat << 1, 0, 0, cam_pos[0], 
               0, 1, 0, cam_pos[1],
               0, 0, 1, cam_pos[2],
               0, 0, 0, 1;

    Eigen::Matrix4f yaw_mat;
    yaw_mat << cos(yaw_rad), 0, -sin(yaw_rad), 0,
               0, 1, 0, 0,
               sin(yaw_rad), 0, cos(yaw_rad), 0,
               0, 0, 0, 1;

    Eigen::Matrix4f pitch_mat;
    pitch_mat << 1, 0, 0, 0,
                 0, cos(pitch_rad), -sin(pitch_rad), 0,
                 0, sin(pitch_rad), cos(pitch_rad), 0,
                 0, 0, 0, 1;
    // clang-format on

    // std::cout << "pos_mat:\n"
    //           << pos_mat << "\nyaw_mat:\n"
    //           << yaw_mat << "\npitch_mat:\n"
    //           << pitch_mat << std::endl;

    Eigen::Matrix4f cam_mat;
    cam_mat = pos_mat * yaw_mat * pitch_mat;

    // std::cout << "cam_mat:\n" << cam_mat << std::endl;

    return cam_mat;
}

/*
|---------y-----------|
x
|---------------------|
 */
// TODO: check this.
Eigen::Vector4f raster2world(uint32_t raster_x, uint32_t raster_y,
                             const resolution_t& target_resolution, const Eigen::Matrix4f& cam_mat)
{
    float disp_ratio
        = static_cast<float>(target_resolution.row) / static_cast<float>(target_resolution.col);

    Eigen::Vector2f screen_cor;
    screen_cor << (1 - ((2 * (raster_x + 0.5)) / target_resolution.row)) * disp_ratio,
        ((2 * (raster_y + 0.5) / target_resolution.col) - 1);

    Eigen::Vector2f cam_cor = screen_cor * std::tan(CAM_FOV * M_PI / 180.0 / 2);
    Eigen::Vector4f cam_pix_cor;
    cam_pix_cor << -cam_cor[1], cam_cor[0], -1, 1;

    Eigen::Vector4f pix_world = cam_mat * cam_pix_cor;

    // std::cout << raster_x << " " << raster_y << "->\n"
    //           << cam_pix_cor << "->\n"
    //           << pix_world << std::endl;

    return pix_world;
}