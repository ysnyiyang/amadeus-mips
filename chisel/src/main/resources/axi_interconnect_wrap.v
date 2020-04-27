module axi_interconnect (
  input         clk,
  input         rst,

  input  [3:0]  s_axi_arid,
  input  [31:0] s_axi_araddr,
  input  [3:0]  s_axi_arlen,
  input  [2:0]  s_axi_arsize,
  input  [1:0]  s_axi_arburst,
  input  [1:0]  s_axi_arlock,
  input  [3:0]  s_axi_arcache,
  input  [2:0]  s_axi_arprot,
  input  [3:0]  s_axi_arqos,
  input         s_axi_arvalid,
  output        s_axi_arready,
  output [3:0]  s_axi_rid,
  output [31:0] s_axi_rdata,
  output [1:0]  s_axi_rresp,
  output        s_axi_rlast,
  output        s_axi_rvalid,
  input         s_axi_rready,
  input  [3:0]  s_axi_awid,
  input  [31:0] s_axi_awaddr,
  input  [3:0]  s_axi_awlen,
  input  [2:0]  s_axi_awsize,
  input  [1:0]  s_axi_awburst,
  input  [1:0]  s_axi_awlock,
  input  [3:0]  s_axi_awcache,
  input  [2:0]  s_axi_awprot,
  input  [3:0]  s_axi_awqos,
  input         s_axi_awvalid,
  output        s_axi_awready,
  input  [3:0]  s_axi_wid,
  input  [31:0] s_axi_wdata,
  input  [3:0]  s_axi_wstrb,
  input         s_axi_wlast,
  input         s_axi_wvalid,
  output        s_axi_wready,
  output [3:0]  s_axi_bid,
  output [1:0]  s_axi_bresp,
  output        s_axi_bvalid,
  input         s_axi_bready,

  output [7:0]  m_axi_arid,
  output [63:0] m_axi_araddr,
  output [7:0]  m_axi_arlen,
  output [5:0]  m_axi_arsize,
  output [3:0]  m_axi_arburst,
  output [3:0]  m_axi_arlock,
  output [7:0]  m_axi_arcache,
  output [5:0]  m_axi_arprot,
  output [7:0]  m_axi_arqos,
  output [1:0]  m_axi_arvalid,
  input  [1:0]  m_axi_arready,
  input  [7:0]  m_axi_rid,
  input  [63:0] m_axi_rdata,
  input  [3:0]  m_axi_rresp,
  input  [1:0]  m_axi_rlast,
  input  [1:0]  m_axi_rvalid,
  output [1:0]  m_axi_rready,
  output [7:0]  m_axi_awid,
  output [63:0] m_axi_awaddr,
  output [7:0]  m_axi_awlen,
  output [5:0]  m_axi_awsize,
  output [3:0]  m_axi_awburst,
  output [3:0]  m_axi_awlock,
  output [7:0]  m_axi_awcache,
  output [5:0]  m_axi_awprot,
  output [7:0]  m_axi_awqos,
  output [1:0]  m_axi_awvalid,
  input  [1:0]  m_axi_awready,
  output [7:0]  m_axi_wid,
  output [63:0] m_axi_wdata,
  output [7:0]  m_axi_wstrb,
  output [1:0]  m_axi_wlast,
  output [1:0]  m_axi_wvalid,
  input  [1:0]  m_axi_wready,
  input  [7:0]  m_axi_bid,
  input  [3:0]  m_axi_bresp,
  input  [1:0]  m_axi_bvalid,
  output [1:0]  m_axi_bready
);

assign m_axi_arqos = 8'd0;
assign m_axi_awqos = 8'd0;



AXIInterconnect axiInterconnect(
  .clock(clk),
  .reset(rst),
  .io_slaves_0_ar_id(s_axi_arid),
  .io_slaves_0_ar_addr(s_axi_araddr),
  .io_slaves_0_ar_len(s_axi_arlen),
  .io_slaves_0_ar_size(s_axi_arsize),
  .io_slaves_0_ar_burst(s_axi_arburst),
  .io_slaves_0_ar_lock(s_axi_arlock),
  .io_slaves_0_ar_cache(s_axi_arcache),
  .io_slaves_0_ar_prot(s_axi_arprot),
  .io_slaves_0_ar_valid(s_axi_arvalid),
  .io_slaves_0_ar_ready(s_axi_arready),
  .io_slaves_0_r_id(s_axi_rid),
  .io_slaves_0_r_data(s_axi_rdata),
  .io_slaves_0_r_resp(s_axi_rresp),
  .io_slaves_0_r_last(s_axi_rlast),
  .io_slaves_0_r_valid(s_axi_rvalid),
  .io_slaves_0_r_ready(s_axi_rready),
  .io_slaves_0_aw_id(s_axi_awid),
  .io_slaves_0_aw_addr(s_axi_awaddr),
  .io_slaves_0_aw_len(s_axi_awlen),
  .io_slaves_0_aw_size(s_axi_awsize),
  .io_slaves_0_aw_burst(s_axi_awburst),
  .io_slaves_0_aw_lock(s_axi_awlock),
  .io_slaves_0_aw_cache(s_axi_awcache),
  .io_slaves_0_aw_prot(s_axi_awprot),
  .io_slaves_0_aw_valid(s_axi_awvalid),
  .io_slaves_0_aw_ready(s_axi_awready),
  .io_slaves_0_w_id(s_axi_wid),
  .io_slaves_0_w_data(s_axi_wdata),
  .io_slaves_0_w_strb(s_axi_wstrb),
  .io_slaves_0_w_last(s_axi_wlast),
  .io_slaves_0_w_valid(s_axi_wvalid),
  .io_slaves_0_w_ready(s_axi_wready),
  .io_slaves_0_b_id(s_axi_bid),
  .io_slaves_0_b_resp(s_axi_bresp),
  .io_slaves_0_b_valid(s_axi_bvalid),
  .io_slaves_0_b_ready(s_axi_bready),

  .io_masters_1_ar_id(m_axi_arid[3:0]),
  .io_masters_1_ar_addr(m_axi_araddr[31:0]),
  .io_masters_1_ar_len(m_axi_arlen[3:0]),
  .io_masters_1_ar_size(m_axi_arsize[2:0]),
  .io_masters_1_ar_burst(m_axi_arburst[1:0]),
  .io_masters_1_ar_lock(m_axi_arlock[1:0]),
  .io_masters_1_ar_cache(m_axi_arcache[3:0]),
  .io_masters_1_ar_prot(m_axi_arprot[2:0]),
  .io_masters_1_ar_valid(m_axi_arvalid[0]),
  .io_masters_1_ar_ready(m_axi_arready[0]),
  .io_masters_1_r_id(m_axi_rid[3:0]),
  .io_masters_1_r_data(m_axi_rdata[31:0]),
  .io_masters_1_r_resp(m_axi_rresp[1:0]),
  .io_masters_1_r_last(m_axi_rlast[0]),
  .io_masters_1_r_valid(m_axi_rvalid[0]),
  .io_masters_1_r_ready(m_axi_rready[0]),
  .io_masters_1_aw_id(m_axi_awid[3:0]),
  .io_masters_1_aw_addr(m_axi_awaddr[31:0]),
  .io_masters_1_aw_len(m_axi_awlen[3:0]),
  .io_masters_1_aw_size(m_axi_awsize[2:0]),
  .io_masters_1_aw_burst(m_axi_awburst[1:0]),
  .io_masters_1_aw_lock(m_axi_awlock[1:0]),
  .io_masters_1_aw_cache(m_axi_awcache[3:0]),
  .io_masters_1_aw_prot(m_axi_awprot[2:0]),
  .io_masters_1_aw_valid(m_axi_awvalid[0]),
  .io_masters_1_aw_ready(m_axi_awready[0]),
  .io_masters_1_w_id(m_axi_wid[3:0]),
  .io_masters_1_w_data(m_axi_wdata[31:0]),
  .io_masters_1_w_strb(m_axi_wstrb[3:0]),
  .io_masters_1_w_last(m_axi_wlast[0]),
  .io_masters_1_w_valid(m_axi_wvalid[0]),
  .io_masters_1_w_ready(m_axi_wready[0]),
  .io_masters_1_b_id(m_axi_bid[3:0]),
  .io_masters_1_b_resp(m_axi_bresp[1:0]),
  .io_masters_1_b_valid(m_axi_bvalid[0]),
  .io_masters_1_b_ready(m_axi_bready[0]),

  .io_masters_0_ar_id(m_axi_arid[7:4]),
  .io_masters_0_ar_addr(m_axi_araddr[63:32]),
  .io_masters_0_ar_len(m_axi_arlen[7:4]),
  .io_masters_0_ar_size(m_axi_arsize[5:3]),
  .io_masters_0_ar_burst(m_axi_arburst[3:2]),
  .io_masters_0_ar_lock(m_axi_arlock[3:2]),
  .io_masters_0_ar_cache(m_axi_arcache[7:4]),
  .io_masters_0_ar_prot(m_axi_arprot[5:3]),
  .io_masters_0_ar_valid(m_axi_arvalid[1]),
  .io_masters_0_ar_ready(m_axi_arready[1]),
  .io_masters_0_r_id(m_axi_rid[7:4]),
  .io_masters_0_r_data(m_axi_rdata[63:32]),
  .io_masters_0_r_resp(m_axi_rresp[3:2]),
  .io_masters_0_r_last(m_axi_rlast[1]),
  .io_masters_0_r_valid(m_axi_rvalid[1]),
  .io_masters_0_r_ready(m_axi_rready[1]),
  .io_masters_0_aw_id(m_axi_awid[7:4]),
  .io_masters_0_aw_addr(m_axi_awaddr[63:32]),
  .io_masters_0_aw_len(m_axi_awlen[7:4]),
  .io_masters_0_aw_size(m_axi_awsize[5:3]),
  .io_masters_0_aw_burst(m_axi_awburst[3:2]),
  .io_masters_0_aw_lock(m_axi_awlock[3:2]),
  .io_masters_0_aw_cache(m_axi_awcache[7:4]),
  .io_masters_0_aw_prot(m_axi_awprot[5:3]),
  .io_masters_0_aw_valid(m_axi_awvalid[1]),
  .io_masters_0_aw_ready(m_axi_awready[1]),
  .io_masters_0_w_id(m_axi_wid[7:4]),
  .io_masters_0_w_data(m_axi_wdata[63:32]),
  .io_masters_0_w_strb(m_axi_wstrb[7:4]),
  .io_masters_0_w_last(m_axi_wlast[1]),
  .io_masters_0_w_valid(m_axi_wvalid[1]),
  .io_masters_0_w_ready(m_axi_wready[1]),
  .io_masters_0_b_id(m_axi_bid[7:4]),
  .io_masters_0_b_resp(m_axi_bresp[3:2]),
  .io_masters_0_b_valid(m_axi_bvalid[1]),
  .io_masters_0_b_ready(m_axi_bready[1])
);

endmodule