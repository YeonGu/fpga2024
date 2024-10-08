
// import "DPI-C" function void tb_mat_read(
//   output int  voxel_pos,
//   output byte density,
//   output byte mvp_mat_0_0,
//   output byte mvp_mat_0_1,
//   output byte mvp_mat_0_2,
//   output byte mvp_mat_1_0,
//   output byte mvp_mat_1_1,
//   output byte mvp_mat_1_2,
//   output byte mvp_mat_2_0,
//   output byte mvp_mat_2_1,
//   output byte mvp_mat_2_2
// );
#include <cassert>
#include <cstdint>
extern "C" void tb_mat_read(int* voxel_pos, unsigned char* density, unsigned char* mvp_mat_0_0,
                            unsigned char* mvp_mat_0_1, unsigned char* mvp_mat_0_2,
                            unsigned char* mvp_mat_1_0, unsigned char* mvp_mat_1_1,
                            unsigned char* mvp_mat_1_2, unsigned char* mvp_mat_2_0,
                            unsigned char* mvp_mat_2_1, unsigned char* mvp_mat_2_2)
{
    // TODO: complete this in testbench to provide data for MIP core.
    static uint64_t voxel_cnt = 0;
    assert(0);
}

// import "DPI-C" function void tb_res_commit(input int screen_pos_x, input int screen_pos_y,
// input int density);
extern "C" void tb_res_commit(int screen_pos_x, int screen_pos_y, int density)
{
    // TODO: complete this in testbench to receive data from MIP core.
    // and display the value on the screen.
    assert(0);
}

extern uint32_t SIM_TIMES;

void sim_stop()
{
    SIM_TIMES = 0;
}