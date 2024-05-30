module ALU_4
(   input [2:0]en,
    input [3:0]a,
    input [3:0]b,
    output reg [3:0]result,
    output reg zero,
    output reg carry,
    output reg overflow,
    output reg flag
);

always @(*)begin
    casez (en)
        3'b000: 
        begin
            addsub addsub1(
                .a(a),
                .b(b),
                .add_sub(0),
                .carry(carry),
                .zero(zero),
                .overflow(overflow),
                .result(result)
            );
        end
        3'b001:
        begin
            addsub addsub1(
                .a(a),
                .b(b),
                .add_sub(1),
                .carry(carry),
                .zero(zero),
                .overflow(overflow),
                .result(result)
            );
        end
        3'b010: result = ~a;
        3'b011: result = a & b;
        3'b100: result = a | b;
        3'b101: result = a ^ b;
        3'b110: 
        3'b111: 

        default: 
    endcase
end

endmodule