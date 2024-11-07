#ifndef __INFO_H__
#define __INFO_H__

#include <eigen3/Eigen/Dense>

struct camera_transform_t
{
    Eigen::Vector3f position;

    float       yaw;
    float       pitch;
    float       ortho_scale; // example: 60.0f. -> l=-30, r=30, b/t depends on aspect ratio
    const float aspect_ratio = 960.0f / 720.0f;
};

#endif // __INFO_H__