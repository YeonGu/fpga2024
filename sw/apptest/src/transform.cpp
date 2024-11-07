#include <eigen3/Eigen/Dense>
#include <info.h>

inline Eigen::Matrix4f gen_mvp_matrix(const camera_transform_t& transform)
{
    // M = Mortho * Mview
    Eigen::Matrix4f mvp_matrix = Eigen::Matrix4f::Identity();

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
    //     [np.cos(yaw_rad), 0, -np.sin(yaw_rad), 0],
    //     [0, 1, 0, 0],
    //     [np.sin(yaw_rad), 0, np.cos(yaw_rad), 0],
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
    rview(0, 2) = -std::sin(yaw);
    rview(2, 0) = std::sin(yaw);
    rview(2, 2) = std::cos(yaw);

    Eigen::Matrix4f pitch_matrix = Eigen::Matrix4f::Identity();

    pitch_matrix(1, 1) = std::cos(pitch);
    pitch_matrix(1, 2) = -std::sin(pitch);
    pitch_matrix(2, 1) = std::sin(pitch);
    pitch_matrix(2, 2) = std::cos(pitch);

    rview = rview * pitch_matrix;
    rview = rview.transpose();

    mvp_matrix = rview * tview;

    // Mortho = Sortho * Tortho

    return mvp_matrix;
}