module vram_tb ();

  reg clk = 0;
  always #1 clk <= ~clk;

  reg [63:0] counter = 0;
  always @(posedge clk) begin
    counter <= counter + 1;
  end

  // Signals for MipVram instantiation
  wire reset;

  wire io_calc_res_0_data_valid;
  wire io_calc_res_0_rden;
  wire [9:0] io_calc_res_0_screen_pos_x;
  wire [9:0] io_calc_res_0_screen_pos_y;
  wire [7:0] io_calc_res_0_density;

  wire io_calc_res_1_data_valid;
  wire io_calc_res_1_rden;
  wire [9:0] io_calc_res_1_screen_pos_x;
  wire [9:0] io_calc_res_1_screen_pos_y;
  wire [7:0] io_calc_res_1_density;

  wire io_calc_res_2_data_valid;
  wire io_calc_res_2_rden;
  wire [9:0] io_calc_res_2_screen_pos_x;
  wire [9:0] io_calc_res_2_screen_pos_y;
  wire [7:0] io_calc_res_2_density;

  wire io_calc_res_3_data_valid;
  wire io_calc_res_3_rden;
  wire [9:0] io_calc_res_3_screen_pos_x;
  wire [9:0] io_calc_res_3_screen_pos_y;
  wire [7:0] io_calc_res_3_density;

  wire io_en_minip;
  wire [31:0] io_ram_port_addra;
  wire [31:0] io_ram_port_dina;
  wire [31:0] io_ram_port_douta;
  wire io_ram_port_ena;
  wire io_ram_port_wea;

  assign reset = counter <= 2;

  // Instantiate the VramTestGen module
  VramTestGen vram_test_gen_inst (
      .clock(clk),
      .reset(reset),
      .io_calc_res_0_data_valid(io_calc_res_0_data_valid),
      .io_calc_res_0_rden(io_calc_res_0_rden),
      .io_calc_res_0_screen_pos_x(io_calc_res_0_screen_pos_x),
      .io_calc_res_0_screen_pos_y(io_calc_res_0_screen_pos_y),
      .io_calc_res_0_density(io_calc_res_0_density),
      .io_calc_res_1_data_valid(io_calc_res_1_data_valid),
      .io_calc_res_1_rden(io_calc_res_1_rden),
      .io_calc_res_1_screen_pos_x(io_calc_res_1_screen_pos_x),
      .io_calc_res_1_screen_pos_y(io_calc_res_1_screen_pos_y),
      .io_calc_res_1_density(io_calc_res_1_density),
      .io_calc_res_2_data_valid(io_calc_res_2_data_valid),
      .io_calc_res_2_rden(io_calc_res_2_rden),
      .io_calc_res_2_screen_pos_x(io_calc_res_2_screen_pos_x),
      .io_calc_res_2_screen_pos_y(io_calc_res_2_screen_pos_y),
      .io_calc_res_2_density(io_calc_res_2_density),
      .io_calc_res_3_data_valid(io_calc_res_3_data_valid),
      .io_calc_res_3_rden(io_calc_res_3_rden),
      .io_calc_res_3_screen_pos_x(io_calc_res_3_screen_pos_x),
      .io_calc_res_3_screen_pos_y(io_calc_res_3_screen_pos_y),
      .io_calc_res_3_density(io_calc_res_3_density),
      .io_en_minip(io_en_minip),
      .io_ram_port_addra(io_ram_port_addra),
      .io_ram_port_dina(io_ram_port_dina),
      .io_ram_port_douta(io_ram_port_douta),
      .io_ram_port_ena(io_ram_port_ena),
      .io_ram_port_wea(io_ram_port_wea)
  );

  wire busy;
  // Instantiate the MipVram module
  MipVram mipvram_inst (
      .clock(clk),
      .reset(reset),
      .io_calc_res_0_data_valid(io_calc_res_0_data_valid),
      .io_calc_res_0_rden(io_calc_res_0_rden),
      .io_calc_res_0_screen_pos_x(io_calc_res_0_screen_pos_x),
      .io_calc_res_0_screen_pos_y(io_calc_res_0_screen_pos_y),
      .io_calc_res_0_density(io_calc_res_0_density),
      .io_calc_res_1_data_valid(io_calc_res_1_data_valid),
      .io_calc_res_1_rden(io_calc_res_1_rden),
      .io_calc_res_1_screen_pos_x(io_calc_res_1_screen_pos_x),
      .io_calc_res_1_screen_pos_y(io_calc_res_1_screen_pos_y),
      .io_calc_res_1_density(io_calc_res_1_density),
      .io_calc_res_2_data_valid(io_calc_res_2_data_valid),
      .io_calc_res_2_rden(io_calc_res_2_rden),
      .io_calc_res_2_screen_pos_x(io_calc_res_2_screen_pos_x),
      .io_calc_res_2_screen_pos_y(io_calc_res_2_screen_pos_y),
      .io_calc_res_2_density(io_calc_res_2_density),
      .io_calc_res_3_data_valid(io_calc_res_3_data_valid),
      .io_calc_res_3_rden(io_calc_res_3_rden),
      .io_calc_res_3_screen_pos_x(io_calc_res_3_screen_pos_x),
      .io_calc_res_3_screen_pos_y(io_calc_res_3_screen_pos_y),
      .io_calc_res_3_density(io_calc_res_3_density),
      .io_en_minip(io_en_minip),
      .io_ram_port_addra(io_ram_port_addra),
      .io_ram_port_dina(io_ram_port_dina),
      .io_ram_port_douta(io_ram_port_douta),
      .io_ram_port_ena(io_ram_port_ena),
      .io_ram_port_wea(io_ram_port_wea),
      .io_ram_reset(reset),
      .io_ram_reset_busy(busy)
  );

endmodule

module randgen (
    input clk,
    input rst,
    output [31:0] data
);

  reg [31:0] data_reg = 0;
  always @(posedge clk) begin

    data_reg <= $random;

  end
  assign data = data_reg;

endmodule
