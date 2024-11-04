// read latency: 1clk
module ultra_vram #(
    parameter VRAM_ADDRA_WIDTH = 17,
    parameter VRAM_ADDRB_WIDTH = 17
) (
    input wire clk,
    input wire rst,
    input wire [VRAM_ADDRA_WIDTH-1:0] addra,
    input wire ena,
    output reg [7:0] douta,
    input wire web,
    input wire [VRAM_ADDRB_WIDTH-1:0] addrb,
    input wire [7:0] dinb,
    output wire wr_reset_busy,
    output wire rd_reset_busy
);

  // Memory array
  reg [7:0] mem[32*1024*3-1:0];

  reg [3:0] rstcnt = 9;
  always @(posedge clk) begin
    if (rst) begin
      rstcnt <= 9;
    end else if (rstcnt > 0) begin
      rstcnt <= rstcnt - 1;
    end
  end
  assign wr_reset_busy = rstcnt > 0;
  assign rd_reset_busy = rstcnt > 2;

  // Port A: Read
  always @(posedge clk) begin
    if (rst) begin
      douta <= 8'b0;
    end else if (ena) begin
      douta <= mem[addra];
    end else begin
      douta <= 8'b0;
    end
  end

  // Port B: Write
  always @(posedge clk) begin
    if (web) begin
      mem[addrb] <= dinb;
    end
  end

endmodule
