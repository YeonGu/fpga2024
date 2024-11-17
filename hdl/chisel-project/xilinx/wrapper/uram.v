/** Build VRAM in Dual Port Mode.
  *
  * @PortA.
  *   Read (din), 8bit (maybe 16?) wide. OREG = true, latency = 1clk
  *
  * @PortB.
  *   Write (dout), 8bit wide
  *
  * @configs.
  *   capacity = 32 * 1024 * 3 Bytes = 96KB
  *   addr width >= lg(32*1024*3 / 8)=14
  */
module ultra_vram #(
    parameter VRAM_ADDRA_WIDTH = 14,
    parameter VRAM_ADDRB_WIDTH = 14
) (
    input wire clk,
    input wire rst,

    input  wire [VRAM_ADDRA_WIDTH-1:0] addra,
    input  wire                        ena,
    output wire [                63:0] douta,  // Changed douta to dina for port A write
    // input wire [                 0:0] wea,    // Added write enable for port A

    input wire [VRAM_ADDRB_WIDTH-1:0] addrb,
    input wire                        enb,    // Added enable for port B read
    input wire [                 7:0] web,
    input wire [                63:0] dinb,

    output wire wr_reset_busy,
    output wire rd_reset_busy
);
  wire dbiterrb;
  wire sbiterrb;
  wire sleep = 1'b0;
  wire injectdbiterra = 1'b0;
  wire injectsbiterra = 1'b0;
  wire regceb = 1'b1;

  assign wr_reset_busy = 1'b0;
  assign rd_reset_busy = 1'b0;

  xpm_memory_sdpram #(
      .ADDR_WIDTH_A      (VRAM_ADDRA_WIDTH),   // Using parameter from module
      .ADDR_WIDTH_B      (VRAM_ADDRB_WIDTH),   // Using parameter from module
      .AUTO_SLEEP_TIME   (0),                  // No auto sleep
      .BYTE_WRITE_WIDTH_A(8),                  // 8-bit byte writes
      .CASCADE_HEIGHT    (1),                  // No cascading
      .CLOCKING_MODE     ("common_clock"),     // Single clock domain
      .ECC_MODE          ("no_ecc"),           // No ECC
      .MEMORY_PRIMITIVE  ("ultra"),            // Using UltraRAM
      .MEMORY_SIZE       (32 * 1024 * 3 * 8),  // 96KB in bits (32K * 3 blocks * 8 bits)
      .READ_DATA_WIDTH_B (64),                 // 8-bit read width
      .READ_LATENCY_B    (3),                  // 3 clock latency as specified
      .RST_MODE_A        ("SYNC"),             // Synchronous reset
      .RST_MODE_B        ("SYNC"),             // Synchronous reset
      .WRITE_DATA_WIDTH_A(64),                 // 32-bit write width
      .WRITE_MODE_B      ("write_first")       // Standard write mode
  ) xpm_memory_sdpram_inst (
      // Port connections remain the same
      .doutb(douta),
      .addra(addrb),
      .addrb(addra),
      .wea  (web),
      .clka (clk),
      .clkb (clk),
      .dina (dinb),
      .ena  (enb),
      .enb  (ena),
      .rstb (rst),

      .dbiterrb(dbiterrb),
      .sbiterrb(sbiterrb),
      .injectdbiterra(injectdbiterra),
      .injectsbiterra(injectsbiterra),
      .regceb(regceb),
      .sleep(sleep)
  );

endmodule
