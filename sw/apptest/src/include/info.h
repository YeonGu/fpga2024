#ifndef __INFO_H__
#define __INFO_H__

#include <eigen3/Eigen/Dense>

struct camera_transform_t
{
    Eigen::Vector3f position;
    float           yaw;
    float           pitch;
};

#endif // __INFO_H__