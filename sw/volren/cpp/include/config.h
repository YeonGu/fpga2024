#ifndef __CONFIG_H__
#define __CONFIG_H__

#include <cstdint>
#define CT_FILEPATH "/home/kasaki352/workspace/contest/fpga2024/sw/volren/male_head_ct/"

#define RAYTRACE_STEP 0.005f

/* camera properties */
#define CAM_FOV 50.0f
#define CAM_POS_X 2.0f
#define CAM_POS_Y 0.0f
#define CAM_POS_Z 2.0f

#define CAM_PITCH 0.0f // up-down
#define CAM_YAW -45.0f // left-right

// #define THRES_SKIN
#define THRES_BONE

/* ray tracing */
#define MAX_TRACE_DISTANCE 5

/* image properties */
#define SLICE_SIZE 1.0f      // in mm
#define PIXEL_SIZE 0.527344f // in mm

struct resolution_t
{
    uint32_t col;
    uint32_t row;
};

#endif