module mipsyswrapper (
    input clock,
    input reset,

    // AXI-Stream MM2S Command Interface
    output [95:0] m_axis_mm2s_cmd_tdata,
    output        m_axis_mm2s_cmd_tvalid,
    input         m_axis_mm2s_cmd_tready,

    // AXI-Stream MM2S Data Interface
    input  [127:0] s_axis_mm2s_tdata,
    input  [ 15:0] s_axis_mm2s_tkeep,
    input          s_axis_mm2s_tlast,
    input          s_axis_mm2s_tvalid,
    output         s_axis_mm2s_tready,

    // AXI-Stream MM2S Status Interface
    input  [7:0] s_axis_mm2s_sts_tdata,
    input        s_axis_mm2s_sts_tvalid,
    output       s_axis_mm2s_sts_tready,
    input        s_axis_mm2s_sts_tkeep,
    input        s_axis_mm2s_sts_tlast,

    // Control Register Interface
    input  [31:0] ctrlreg_port_addra,
    input  [31:0] ctrlreg_port_dina,
    output [31:0] ctrlreg_port_douta,
    input         ctrlreg_port_ena,
    input  [ 3:0] ctrlreg_port_wea,

    // VRAM Interface
    input  [63:0] vram_port_addra,
    input  [63:0] vram_port_dina,
    output [63:0] vram_port_douta,
    input         vram_port_ena,
    input  [ 7:0] vram_port_wea
);

  MipSystem mipsys_inst (
      .clock                    (clock),
      .reset                    (reset),
      .io_m_axis_mm2s_cmd_tdata (m_axis_mm2s_cmd_tdata),
      .io_m_axis_mm2s_cmd_tvalid(m_axis_mm2s_cmd_tvalid),
      .io_m_axis_mm2s_cmd_tready(m_axis_mm2s_cmd_tready),
      .io_s_axis_mm2s_tdata     (s_axis_mm2s_tdata),
      .io_s_axis_mm2s_tkeep     (s_axis_mm2s_tkeep),
      .io_s_axis_mm2s_tlast     (s_axis_mm2s_tlast),
      .io_s_axis_mm2s_tvalid    (s_axis_mm2s_tvalid),
      .io_s_axis_mm2s_tready    (s_axis_mm2s_tready),
      .io_s_axis_mm2s_sts_tdata (s_axis_mm2s_sts_tdata),
      .io_s_axis_mm2s_sts_tvalid(s_axis_mm2s_sts_tvalid),
      .io_s_axis_mm2s_sts_tready(s_axis_mm2s_sts_tready),
      .io_s_axis_mm2s_sts_tkeep (s_axis_mm2s_sts_tkeep),
      .io_s_axis_mm2s_sts_tlast (s_axis_mm2s_sts_tlast),
      .io_ctrlreg_port_addra    (ctrlreg_port_addra),
      .io_ctrlreg_port_dina     (ctrlreg_port_dina),
      .io_ctrlreg_port_douta    (ctrlreg_port_douta),
      .io_ctrlreg_port_ena      (ctrlreg_port_ena),
      .io_ctrlreg_port_wea      (ctrlreg_port_wea),
      .io_vram_port_addra       (vram_port_addra),
      .io_vram_port_dina        (vram_port_dina),
      .io_vram_port_douta       (vram_port_douta),
      .io_vram_port_ena         (vram_port_ena),
      .io_vram_port_wea         (vram_port_wea)
  );

endmodule
