module mip_dispatch_fifo #(
    parameter DATA_WIDTH = 128,
    parameter FIFO_DEPTH = 1024
) (
    input wire clk,
    input wire srst,

    input wire wr_en,
    input wire [DATA_WIDTH-1:0] din,
    output wire full,

    input wire rd_en,
    output reg [DATA_WIDTH-1:0] dout,
    output wire empty,

    output wire [9:0] data_count,
    output valid,
    output [10:0] wr_data_count,
    output [10:0] rd_data_count,

    output wr_rst_busy,
    output rd_rst_busy
);

  reg [DATA_WIDTH-1:0] memory[0:FIFO_DEPTH-1];
  reg [$clog2(FIFO_DEPTH):0] wr_ptr;
  reg [$clog2(FIFO_DEPTH):0] rd_ptr;
  reg [$clog2(FIFO_DEPTH):0] count;

  assign full = (count == FIFO_DEPTH);
  assign empty = (count == 0);
  assign data_count = count[9:0];
  assign valid = ~empty;
  assign wr_data_count = count[10:0];
  assign rd_data_count = count[10:0];

  assign wr_rst_busy = srst;
  assign rd_rst_busy = srst;

  always @(posedge clk) begin
    if (srst) begin
      wr_ptr <= 0;
      rd_ptr <= 0;
      count  <= 0;
      dout   <= 0;
    end else begin
      if (wr_en && !full) begin
        memory[wr_ptr[($clog2(FIFO_DEPTH)-1):0]] <= din;
        wr_ptr <= wr_ptr + 1;
      end

      if (rd_en && !empty) begin
        dout   <= memory[rd_ptr[($clog2(FIFO_DEPTH)-1):0]];
        rd_ptr <= rd_ptr + 1;
      end

      if (wr_en && !full && !(rd_en && !empty)) count <= count + 1;
      else if (!wr_en && rd_en && !empty) count <= count - 1;
    end
  end

endmodule
