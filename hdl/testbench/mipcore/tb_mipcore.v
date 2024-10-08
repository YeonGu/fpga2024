/*********************************************************************
  *  @file     tb_mipcore.v
  *  @brief    Testbench for mipcore

  * refer to https://soc.ustc.edu.cn/CECS/lab2/verilator/ for details in verilator and DPI-C  

 *********************************************************************/


module testbench ();

  reg clock = 0;
  always #1 clock = ~clock;

  wire reset = 0;

  reg [31:0] voxel_pos;
  reg [7:0] density;
  reg [7:0] mat00, mat01, mat02;
  reg [7:0] mat10, mat11, mat12;
  reg [7:0] mat20, mat21, mat22;

  reg valid;

  always @(posedge clock) begin
    if (!reset) begin
      tb_mat_read(voxel_pos, density, mat00, mat01, mat02, mat10, mat11, mat12, mat20, mat21,
                  mat22);
      valid <= 1;
    end else begin
      valid <= 0;
    end
  end

  wire stall = 0;

  wire out_valid;
  wire [7:0] out_density;
  wire [10:0] out_screenPos_x;
  wire [10:0] out_screenPos_y;

  always @(posedge clock) begin
    if (out_valid) begin
      tb_res_commit(out_screenPos_x, out_screenPos_y, out_density);
    end
  end

  IntensityProjectionCore mip_core_inst (
      .clock(clock),  // input         clock,
      .reset(reset),  //               reset,
      .in_valid(valid),  //               in_valid,
      .in_pipelineStall(stall),  //               in_pipelineStall,
      .in_density(density),  // input  [7:0]  in_density,
      .in_voxelPos(voxel_pos[15:0]),  // input  [15:0] in_voxelPos,
      .in_mvpInfo_mat_0_0(mat00),  // input  [7:0]  in_mvpInfo_mat_0_0,
      .in_mvpInfo_mat_0_1(mat01),  //               in_mvpInfo_mat_0_1,
      .in_mvpInfo_mat_0_2(mat02),  //               in_mvpInfo_mat_0_2,
      .in_mvpInfo_mat_1_0(mat10),  //               in_mvpInfo_mat_1_0,
      .in_mvpInfo_mat_1_1(mat11),  //               in_mvpInfo_mat_1_1,
      .in_mvpInfo_mat_1_2(mat12),  //               in_mvpInfo_mat_1_2,
      .in_mvpInfo_mat_2_0(mat20),  //               in_mvpInfo_mat_2_0,
      .in_mvpInfo_mat_2_1(mat21),  //               in_mvpInfo_mat_2_1,
      .in_mvpInfo_mat_2_2(mat22),  //               in_mvpInfo_mat_2_2,
      .out_valid(out_valid),  // output        out_valid,
      .out_density(out_density),  // output [7:0]  out_density,
      .out_screenPos_x(out_screenPos_x),  // output [10:0] out_screenPos_x,
      .out_screenPos_y(out_screenPos_y)  //               out_screenPos_y
  );
endmodule

import "DPI-C" function void tb_mat_read(
  output int  voxel_pos,
  output byte density,
  output byte mvp_mat_0_0,
  output byte mvp_mat_0_1,
  output byte mvp_mat_0_2,
  output byte mvp_mat_1_0,
  output byte mvp_mat_1_1,
  output byte mvp_mat_1_2,
  output byte mvp_mat_2_0,
  output byte mvp_mat_2_1,
  output byte mvp_mat_2_2
);
import "DPI-C" function void tb_res_commit(
  input int screen_pos_x,
  input int screen_pos_y,
  input int density
);
