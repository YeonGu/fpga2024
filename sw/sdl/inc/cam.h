#ifndef __CAM_H__
#define __CAM_H__

#include <Eigen/Dense>

struct Camera {
    int width, height;
    float yaw;
    float pitch;
    const float ortho_scale = 80.f;
    const float aspect_ratio;
    const float delta_angle = 3.f;
    const float camera_speed = 0.05f;
    Eigen::Vector3f pos, up, look_at;
    Eigen::Matrix4i ortho_proj_mat;

    Camera(int w, int h)
        : width(w), height(h), yaw(0.f), pitch(0.f), aspect_ratio((float)w / h),
          pos(Eigen::Vector3f(0.f, 0.f, 0.f)),
          up(Eigen::Vector3f(0.f, 1.f, 0.f)),
          look_at(Eigen::Vector3f(0.f, 0.f, -1.f)) {
        update_mvp_matrix();
    }

    void update_mvp_matrix();
};

inline uint32_t float2fixed(float f) {
    float scaled = std::round(f * 64.f);
    return static_cast<uint32_t>(scaled) & 0xFFFF0000;
}

inline void Camera::update_mvp_matrix() {
    // M = Mortho * Mview
    Eigen::Matrix4f Mview = Eigen::Matrix4f::Identity();

    // Mview = R * T
    Eigen::Matrix4f tview = Eigen::Matrix4f::Identity();
    Eigen::Matrix4f rview = Eigen::Matrix4f::Identity();

    tview(0, 3) = -pos.x();
    tview(1, 3) = -pos.y();
    tview(2, 3) = -pos.z();

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
    float yaw_rad = yaw * M_PI / 180.f;
    rview(0, 0) = std::cos(yaw_rad);
    rview(0, 2) = std::sin(yaw_rad);
    rview(2, 0) = -std::sin(yaw_rad);
    rview(2, 2) = std::cos(yaw_rad);

    Eigen::Matrix4f pitch_matrix = Eigen::Matrix4f::Identity();

    pitch_matrix(1, 1) = std::cos(pitch);
    pitch_matrix(1, 2) = -std::sin(pitch);
    pitch_matrix(2, 1) = std::sin(pitch);
    pitch_matrix(2, 2) = std::cos(pitch);

    rview = rview * pitch_matrix;
    rview = rview.transpose();

    Mview = rview * tview;

    // Mortho = Sortho * Tortho
    float l = -ortho_scale / 2;
    float r = ortho_scale / 2;
    float b = -ortho_scale / 2 / aspect_ratio;
    float t = ortho_scale / 2 / aspect_ratio;
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

    Eigen::Matrix4f tmp = Mortho * Mview;
    Eigen::Matrix4i result;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            result(i, j) = float2fixed(tmp(i, j));
        }
    }
    ortho_proj_mat = result;
}
#endif // __CAM_H__