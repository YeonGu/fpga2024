#include <eigen3/Eigen/Dense>
#include <info.h>
#include <iostream>

uint32_t float2fixed(float f)
{
    float scaled = std::round(f * 64.f);
    return static_cast<uint32_t>(scaled) & 0xFFFF0000;
}

Eigen::Matrix4f gen_mvp_matrix(const camera_transform_t& transform)
{
    // M = Mortho * Mview
    Eigen::Matrix4f Mview = Eigen::Matrix4f::Identity();

    // Mview = R * T
    Eigen::Matrix4f tview = Eigen::Matrix4f::Identity();
    Eigen::Matrix4f rview = Eigen::Matrix4f::Identity();

    tview(0, 3) = -transform.position.x();
    tview(1, 3) = -transform.position.y();
    tview(2, 3) = -transform.position.z();

    float yaw   = transform.yaw;
    float pitch = transform.pitch;

    // R^-1 = Myaw * Mpitch
    // # YAW
    // [
    //     [np.cos(yaw_rad), 0, np.sin(yaw_rad), 0],
    //     [0, 1, 0, 0],
    //     [-np.sin(yaw_rad), 0, np.cos(yaw_rad), 0],
    //     [0, 0, 0, 1],
    // ]
    // # PITCH
    // [
    //     [1, 0, 0, 0],
    //     [0, np.cos(pitch_rad), -np.sin(pitch_rad), 0],
    //     [0, np.sin(pitch_rad), np.cos(pitch_rad), 0],
    //     [0, 0, 0, 1],
    // ]
    rview(0, 0) = std::cos(yaw);
    rview(0, 2) = std::sin(yaw);
    rview(2, 0) = -std::sin(yaw);
    rview(2, 2) = std::cos(yaw);

    Eigen::Matrix4f pitch_matrix = Eigen::Matrix4f::Identity();

    pitch_matrix(1, 1) = std::cos(pitch);
    pitch_matrix(1, 2) = -std::sin(pitch);
    pitch_matrix(2, 1) = std::sin(pitch);
    pitch_matrix(2, 2) = std::cos(pitch);

    rview        = rview * pitch_matrix;
    auto rview_t = rview.transpose();

    Mview = rview_t * tview;
    std::cout << Mview << std::endl;

    // Mortho = Sortho * Tortho
    float       l = -transform.ortho_scale / 2;
    float       r = transform.ortho_scale / 2;
    float       b = -transform.ortho_scale / 2 / transform.aspect_ratio;
    float       t = transform.ortho_scale / 2 / transform.aspect_ratio;
    const float n = 0.0f;
    const float f = -64.0f;

    // clang-format off
    Eigen::Matrix4f Sortho = Eigen::Matrix4f::Identity();
    Eigen::Matrix4f Tortho = Eigen::Matrix4f::Identity();
    Sortho <<   2 / (r - l), 0, 0, 0, 
                0, 2 / (t - b), 0, 0, 
                0, 0, 2 / (n - f), 0, 
                0, 0, 0, 1;
    Tortho <<   1, 0, 0, -(r + l) / 2, 
                0, 1, 0, -(t + b) / 2, 
                0, 0, 1, -(n + f) / 2, 
                0, 0, 0, 1;
    // clang-format on
    Eigen::Matrix4f Mortho = Sortho * Tortho;

    std::cout << Mortho << std::endl;

    Eigen::Matrix4f tmp = Mortho * Mview;

    Eigen::Matrix4f result;
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            result(i, j) = float2fixed(tmp(i, j));
        }
    }
    return tmp;
}