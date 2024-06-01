// Generated by CIRCT firtool-1.62.0
module SharedBus(
  input         clock,
                reset,
  input  [31:0] cpu_data_interface_adr_o,
                cpu_data_interface_dat_o,
  input  [3:0]  cpu_data_interface_sel_o,
  input         cpu_data_interface_we_o,
                cpu_data_interface_stb_o,
                cpu_data_interface_cyc_o,
  output [31:0] cpu_data_interface_dat_i,
  output        cpu_data_interface_ack_i,
                cpu_data_interface_err_i,
  input  [31:0] cpu_instruction_interface_adr_o,
                cpu_instruction_interface_dat_o,
  input  [3:0]  cpu_instruction_interface_sel_o,
  input         cpu_instruction_interface_we_o,
                cpu_instruction_interface_stb_o,
                cpu_instruction_interface_cyc_o,
  output [31:0] cpu_instruction_interface_dat_i,
  output        cpu_instruction_interface_ack_i,
                cpu_instruction_interface_err_i,
  output [31:0] main_memory_adr_i,
                main_memory_dat_i,
  output [3:0]  main_memory_sel_i,
  output        main_memory_we_i,
                main_memory_stb_i,
                main_memory_cyc_i,
  input  [31:0] main_memory_dat_o,
  input         main_memory_ack_o,
                main_memory_err_o,
  output [31:0] led_matrix_0_adr_i,
                led_matrix_0_dat_i,
  output [3:0]  led_matrix_0_sel_i,
  output        led_matrix_0_we_i,
                led_matrix_0_stb_i,
                led_matrix_0_cyc_i,
  input  [31:0] led_matrix_0_dat_o,
  input         led_matrix_0_ack_o,
                led_matrix_0_err_o,
  output [31:0] switches_0_adr_i,
                switches_0_dat_i,
  output [3:0]  switches_0_sel_i,
  output        switches_0_we_i,
                switches_0_stb_i,
                switches_0_cyc_i,
  input  [31:0] switches_0_dat_o,
  input         switches_0_ack_o,
                switches_0_err_o,
  output        invalid_address
);

  reg         cpu_data_interface_mask;
  reg         cpu_instruction_interface_mask;
  wire        unmasked_grant_1 =
    ~cpu_data_interface_cyc_o & cpu_instruction_interface_cyc_o;
  wire        masked_grant_0 = cpu_data_interface_cyc_o & cpu_data_interface_mask;
  wire        masked_grant_1 =
    ~masked_grant_0 & cpu_instruction_interface_cyc_o & cpu_instruction_interface_mask;
  wire [1:0]  masked_grant_vec = {masked_grant_1, masked_grant_0};
  wire [1:0]  unmasked_grant_vec = {unmasked_grant_1, cpu_data_interface_cyc_o};
  reg         cpu_data_interface_grant;
  reg         cpu_instruction_interface_grant;
  reg         cyc_out;
  wire        _GEN = (|unmasked_grant_vec) & unmasked_grant_1;
  wire        masterSelect = (|masked_grant_vec) ? masked_grant_1 : _GEN;
  wire [31:0] adr =
    masterSelect ? cpu_instruction_interface_adr_o : cpu_data_interface_adr_o;
  wire [31:0] dat_w =
    masterSelect ? cpu_instruction_interface_dat_o : cpu_data_interface_dat_o;
  wire [3:0]  sel =
    masterSelect ? cpu_instruction_interface_sel_o : cpu_data_interface_sel_o;
  wire        we =
    masterSelect ? cpu_instruction_interface_we_o : cpu_data_interface_we_o;
  wire        amcp_0 = adr[31:28] != 4'hF;
  wire        dat_r_lower_bound_1 = adr > 32'hEFFFFFFF;
  wire        amcp_1 = dat_r_lower_bound_1 & adr < 32'hF0000DAC;
  wire        dat_r_lower_bound_2 = adr > 32'hF0000DAB;
  wire        amcp_2 = dat_r_lower_bound_2 & adr < 32'hF0000DB0;
  wire [31:0] dat_r =
    adr[31:28] != 4'hF
      ? main_memory_dat_o
      : dat_r_lower_bound_1 & adr < 32'hF0000DAC
          ? led_matrix_0_dat_o
          : dat_r_lower_bound_2 & adr < 32'hF0000DB0
              ? switches_0_dat_o
              : main_memory_dat_o;
  wire        ack = main_memory_ack_o | led_matrix_0_ack_o | switches_0_ack_o;
  wire        err = main_memory_err_o | led_matrix_0_err_o | switches_0_err_o;
  wire        cyc_validated_stb =
    (masterSelect ? cpu_instruction_interface_stb_o : cpu_data_interface_stb_o) & cyc_out;
  always @(posedge clock) begin
    if (reset) begin
      cpu_data_interface_mask <= 1'h0;
      cpu_instruction_interface_mask <= 1'h0;
      cpu_data_interface_grant <= 1'h0;
      cpu_instruction_interface_grant <= 1'h0;
      cyc_out <= 1'h0;
    end
    else begin
      if ((|masked_grant_vec) | (|unmasked_grant_vec))
        cpu_data_interface_mask <= ~masterSelect;
      cpu_instruction_interface_mask <=
        (|masked_grant_vec) | (|unmasked_grant_vec) | cpu_instruction_interface_mask;
      cpu_data_interface_grant <=
        (|masked_grant_vec)
          ? masked_grant_0
          : (|unmasked_grant_vec) & cpu_data_interface_cyc_o;
      cpu_instruction_interface_grant <= (|masked_grant_vec) ? masked_grant_1 : _GEN;
      cyc_out <= |{cpu_instruction_interface_cyc_o, cpu_data_interface_cyc_o};
    end
  end // always @(posedge)
  assign cpu_data_interface_dat_i = dat_r;
  assign cpu_data_interface_ack_i = ack & cpu_data_interface_grant;
  assign cpu_data_interface_err_i = err & cpu_data_interface_grant;
  assign cpu_instruction_interface_dat_i = dat_r;
  assign cpu_instruction_interface_ack_i = ack & cpu_instruction_interface_grant;
  assign cpu_instruction_interface_err_i = err & cpu_instruction_interface_grant;
  assign main_memory_adr_i = adr;
  assign main_memory_dat_i = dat_w;
  assign main_memory_sel_i = sel;
  assign main_memory_we_i = we;
  assign main_memory_stb_i = cyc_validated_stb & amcp_0;
  assign main_memory_cyc_i = cyc_out;
  assign led_matrix_0_adr_i = adr;
  assign led_matrix_0_dat_i = dat_w;
  assign led_matrix_0_sel_i = sel;
  assign led_matrix_0_we_i = we;
  assign led_matrix_0_stb_i = cyc_validated_stb & amcp_1;
  assign led_matrix_0_cyc_i = cyc_out;
  assign switches_0_adr_i = adr;
  assign switches_0_dat_i = dat_w;
  assign switches_0_sel_i = sel;
  assign switches_0_we_i = we;
  assign switches_0_stb_i = cyc_validated_stb & amcp_2;
  assign switches_0_cyc_i = cyc_out;
  assign invalid_address = ~(amcp_0 | amcp_1 | amcp_2);
endmodule

