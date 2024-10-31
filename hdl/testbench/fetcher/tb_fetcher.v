module tb_fetcher ();

  reg clk;
  reg reset;
  always #1 clk <= ~clk;
  initial begin
    reset = 1;
    #9;
    reset = 0;
  end

  localparam IDLE = 0, START = 1, STARTED = 2;
  reg [3:0] state;

  reg io_ctrl_start_valid;
  wire io_ctrl_start_ready;

  wire [71:0] io_m_axis_mm2s_cmd_tdata;
  wire io_m_axis_mm2s_cmd_tvalid;
  reg io_m_axis_mm2s_cmd_tready;
  reg [127:0] io_s_axis_mm2s_tdata;
  reg [15:0] io_s_axis_mm2s_tkeep;
  reg io_s_axis_mm2s_tlast;
  reg io_s_axis_mm2s_tvalid;
  wire io_s_axis_mm2s_tready;

  reg io_mip_channels_0_need_data;
  wire io_mip_channels_0_voxel_addr_reg_wren;
  wire [19:0] io_mip_channels_0_voxel_addr_reg_wrdata;
  wire io_mip_channels_0_proc_queue_wren;
  wire [127:0] io_mip_channels_0_proc_queue_wrdata;
  reg io_mip_channels_1_need_data;
  wire io_mip_channels_1_voxel_addr_reg_wren;
  wire [19:0] io_mip_channels_1_voxel_addr_reg_wrdata;
  wire io_mip_channels_1_proc_queue_wren;
  wire [127:0] io_mip_channels_1_proc_queue_wrdata;
  reg io_mip_channels_2_need_data;
  wire io_mip_channels_2_voxel_addr_reg_wren;
  wire [19:0] io_mip_channels_2_voxel_addr_reg_wrdata;
  wire io_mip_channels_2_proc_queue_wren;
  wire [127:0] io_mip_channels_2_proc_queue_wrdata;
  reg io_mip_channels_3_need_data;
  wire io_mip_channels_3_voxel_addr_reg_wren;
  wire [19:0] io_mip_channels_3_voxel_addr_reg_wrdata;
  wire io_mip_channels_3_proc_queue_wren;
  wire [127:0] io_mip_channels_3_proc_queue_wrdata;

  initial begin
    io_mip_channels_0_need_data = 0;
    #1701;
    io_mip_channels_0_need_data = 1;
    #2;
    io_mip_channels_0_need_data = 0;
  end
  initial begin
    io_mip_channels_1_need_data = 0;
    #2299;
    io_mip_channels_1_need_data = 1;
    #2;
    io_mip_channels_1_need_data = 0;
  end
  initial begin
    io_mip_channels_2_need_data = 0;
    #2399;
    io_mip_channels_2_need_data = 1;
    #6000;
    io_mip_channels_2_need_data = 0;
  end
  initial begin
    io_mip_channels_3_need_data = 0;
    #2897;
    io_mip_channels_3_need_data = 1;
    #2;
    io_mip_channels_3_need_data = 0;
  end

  always @(posedge clk) begin
    if (reset) begin
      state <= IDLE;
      io_ctrl_start_valid <= 0;
    end else begin
      case (state)
        IDLE: state <= START;

        START: begin
          state <= io_ctrl_start_ready && io_ctrl_start_valid ? STARTED : START;
        end

        STARTED: begin
          state <= STARTED;
        end
        default: state <= IDLE;
      endcase
      io_ctrl_start_valid <= state == START;
    end
  end


  MipDataFetcher uut (
      .clock(clk),
      .reset(reset),
      .io_ctrl_start_valid(io_ctrl_start_valid),
      .io_ctrl_start_ready(io_ctrl_start_ready),
      .io_m_axis_mm2s_cmd_tdata(io_m_axis_mm2s_cmd_tdata),
      .io_m_axis_mm2s_cmd_tvalid(io_m_axis_mm2s_cmd_tvalid),
      .io_m_axis_mm2s_cmd_tready(io_m_axis_mm2s_cmd_tready),
      .io_s_axis_mm2s_tdata(io_s_axis_mm2s_tdata),
      .io_s_axis_mm2s_tkeep(io_s_axis_mm2s_tkeep),
      .io_s_axis_mm2s_tlast(io_s_axis_mm2s_tlast),
      .io_s_axis_mm2s_tvalid(io_s_axis_mm2s_tvalid),
      .io_s_axis_mm2s_tready(io_s_axis_mm2s_tready),
      .io_mip_channels_0_need_data(io_mip_channels_0_need_data),
      .io_mip_channels_0_voxel_addr_reg_wren(io_mip_channels_0_voxel_addr_reg_wren),
      .io_mip_channels_0_voxel_addr_reg_wrdata(io_mip_channels_0_voxel_addr_reg_wrdata),
      .io_mip_channels_0_proc_queue_wren(io_mip_channels_0_proc_queue_wren),
      .io_mip_channels_0_proc_queue_wrdata(io_mip_channels_0_proc_queue_wrdata),
      .io_mip_channels_1_need_data(io_mip_channels_1_need_data),
      .io_mip_channels_1_voxel_addr_reg_wren(io_mip_channels_1_voxel_addr_reg_wren),
      .io_mip_channels_1_voxel_addr_reg_wrdata(io_mip_channels_1_voxel_addr_reg_wrdata),
      .io_mip_channels_1_proc_queue_wren(io_mip_channels_1_proc_queue_wren),
      .io_mip_channels_1_proc_queue_wrdata(io_mip_channels_1_proc_queue_wrdata),
      .io_mip_channels_2_need_data(io_mip_channels_2_need_data),
      .io_mip_channels_2_voxel_addr_reg_wren(io_mip_channels_2_voxel_addr_reg_wren),
      .io_mip_channels_2_voxel_addr_reg_wrdata(io_mip_channels_2_voxel_addr_reg_wrdata),
      .io_mip_channels_2_proc_queue_wren(io_mip_channels_2_proc_queue_wren),
      .io_mip_channels_2_proc_queue_wrdata(io_mip_channels_2_proc_queue_wrdata),
      .io_mip_channels_3_need_data(io_mip_channels_3_need_data),
      .io_mip_channels_3_voxel_addr_reg_wren(io_mip_channels_3_voxel_addr_reg_wren),
      .io_mip_channels_3_voxel_addr_reg_wrdata(io_mip_channels_3_voxel_addr_reg_wrdata),
      .io_mip_channels_3_proc_queue_wren(io_mip_channels_3_proc_queue_wren),
      .io_mip_channels_3_proc_queue_wrdata(io_mip_channels_3_proc_queue_wrdata)
  );

  axi_datamover_sim axi_datamover_inst (
      .clock(clk),
      .reset(reset),
      .io_s_axis_mm2s_cmd_tdata(io_m_axis_mm2s_cmd_tdata),
      .io_s_axis_mm2s_cmd_tvalid(io_m_axis_mm2s_cmd_tvalid),
      .io_s_axis_mm2s_cmd_tready(io_m_axis_mm2s_cmd_tready),
      .io_m_axis_mm2s_tdata(io_s_axis_mm2s_tdata),
      .io_m_axis_mm2s_tkeep(io_s_axis_mm2s_tkeep),
      .io_m_axis_mm2s_tlast(io_s_axis_mm2s_tlast),
      .io_m_axis_mm2s_tvalid(io_s_axis_mm2s_tvalid),
      .io_m_axis_mm2s_tready(io_s_axis_mm2s_tready)
  );
endmodule

/* verilator lint_off LATCH */
/* verilator lint_off SYNCASYNCNET */
module memory_read (
    input en,
    input [31:0] addr,
    output [127:0] data
);

  wire [31:0] read_addr = addr;
  wire [63:0] dh_data;
  wire [63:0] dl_data;
  assign data = {dh_data, dl_data};

  always @(*) begin
    read_128bit_data(read_addr, dh_data[63:32], dh_data[31:0], dl_data[63:32], dl_data[31:0]);
  end

endmodule


import "DPI-C" function void read_128bit_data(
  input  [31:0] addr,
  output [31:0] data_h,
  output [31:0] data_h2,
  output [31:0] data_h3,
  output [31:0] data_l
);
