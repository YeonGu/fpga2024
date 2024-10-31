module mip_dispatch_fifo #(
    parameter DATA_WIDTH = 128,
    parameter FIFO_DEPTH = 1024
) (
    input wire clk,
    input wire srst,

    input wire wr_en,
    input wire [DATA_WIDTH-1:0] wr_data,
    output wire full,

    input wire rd_en,
    output reg [DATA_WIDTH-1:0] rd_data,
    output wire empty,

    output wire [9:0] data_count
);

  reg [DATA_WIDTH-1:0] fifo_mem[0:FIFO_DEPTH-1];
  reg [9:0] wr_ptr;
  reg [9:0] rd_ptr;
  reg [9:0] count;

  assign full = (count == FIFO_DEPTH - 1);
  assign empty = (count == 0);
  assign data_count = count;

  always @(posedge clk) begin
    if (srst) begin
      wr_ptr <= 0;
      rd_ptr <= 0;
      count  <= 0;
    end else begin
      if (wr_en && !full) begin
        fifo_mem[wr_ptr] <= wr_data;
        wr_ptr <= wr_ptr + 1;
        count <= count + 1;
      end
      if (rd_en && !empty) begin
        rd_data <= fifo_mem[rd_ptr];
        rd_ptr  <= rd_ptr + 1;
        count   <= count - 1;
      end
    end
  end

endmodule
