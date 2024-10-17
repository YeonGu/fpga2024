package rendering

import chisel3._
import chisel3.util._

import CUParams._

class FloatPoint extends Bundle {
    val sign = Bool()
    val int  = UInt(5.W)
    val sig  = UInt(10.W)
}

object FloatPoint {
    def apply(sign: Bool, int: UInt, sig: UInt): FloatPoint = {
        val result = Wire(new FloatPoint())
        result.sign := sign
        result.int  := int
        result.sig  := sig
        result
    }
}
class Fixed2Float extends BlackBox {
    val io = IO(new Bundle {
        val s_axis_a_tvalid      = Input(Bool())
        val s_axis_a_tdata       = Input(SInt(INTERMEDIATE_WIDTH.W))
        val m_axis_result_tvalid = Output(Bool())
        val m_axis_result_tdata  = Output(SInt(16.W))
    })
}

object Fixed2Float {
    def apply(a: SInt): FloatPoint = {
        val result = Wire(new FloatPoint())
        val f2f    = Module(new Fixed2Float())
        f2f.io.s_axis_a_tvalid := true.B
        f2f.io.s_axis_a_tdata  := a
        result.sign            := f2f.io.m_axis_result_tdata(15)
        result.int             := f2f.io.m_axis_result_tdata(14, 10)
        result.sig             := f2f.io.m_axis_result_tdata(9, 0)
        result
    }
}

class FloatMul extends BlackBox {
    val io = IO(new Bundle {
        val aclk                 = Input(Clock())
        val s_axis_a_tdata       = Input(SInt(16.W))
        val s_axis_a_tvalid      = Input(Bool())
        val s_axis_b_tdata       = Input(SInt(16.W))
        val s_axis_b_tvalid      = Input(Bool())
        val m_axis_result_tdata  = Output(SInt(16.W))
        val m_axis_result_tvalid = Output(Bool())
    })
}

object FloatMul {
    def apply(a: FloatPoint, b: FloatPoint, clk: Clock): FloatPoint = {
        val result = Wire(new FloatPoint())
        val fm     = Module(new FloatMul())
        fm.io.aclk            := clk
        fm.io.s_axis_a_tdata  := Cat(a.sign, a.int, a.sig).asSInt
        fm.io.s_axis_a_tvalid := true.B
        fm.io.s_axis_b_tdata  := Cat(b.sign, b.int, b.sig).asSInt
        fm.io.s_axis_b_tvalid := true.B
        result.sign           := fm.io.m_axis_result_tdata(15)
        result.int            := fm.io.m_axis_result_tdata(14, 10)
        result.sig            := fm.io.m_axis_result_tdata(9, 0)
        result
    }
}

class FloatAdd extends BlackBox {
    val io = IO(new Bundle {
        val aclk                 = Input(Clock())
        val s_axis_a_tdata       = Input(SInt(16.W))
        val s_axis_a_tvalid      = Input(Bool())
        val s_axis_b_tdata       = Input(SInt(16.W))
        val s_axis_b_tvalid      = Input(Bool())
        val m_axis_result_tdata  = Output(SInt(16.W))
        val m_axis_result_tvalid = Output(Bool())
    })
}

object FloatAdd {
    def apply(a: FloatPoint, b: FloatPoint, clk: Clock): FloatPoint = {
        val result = Wire(new FloatPoint())
        val fa     = Module(new FloatAdd())
        fa.io.aclk            := clk
        fa.io.s_axis_a_tdata  := Cat(a.sign, a.int, a.sig).asSInt
        fa.io.s_axis_a_tvalid := true.B
        fa.io.s_axis_b_tdata  := Cat(b.sign, b.int, b.sig).asSInt
        fa.io.s_axis_b_tvalid := true.B
        result.sign           := fa.io.m_axis_result_tdata(15)
        result.int            := fa.io.m_axis_result_tdata(14, 10)
        result.sig            := fa.io.m_axis_result_tdata(9, 0)
        result
    }
}

class FloatDiv extends BlackBox {
    val io = IO(new Bundle {
        val aclk                 = Input(Clock())
        val s_axis_a_tdata       = Input(SInt(16.W))
        val s_axis_a_tvalid      = Input(Bool())
        val s_axis_b_tdata       = Input(SInt(16.W))
        val s_axis_b_tvalid      = Input(Bool())
        val m_axis_result_tdata  = Output(SInt(16.W))
        val m_axis_result_tvalid = Output(Bool())
    })
}

object FloatDiv {
    def apply(a: FloatPoint, b: FloatPoint, clk: Clock): FloatPoint = {
        val result = Wire(new FloatPoint())
        val fd     = Module(new FloatDiv())
        fd.io.aclk            := clk
        fd.io.s_axis_a_tdata  := Cat(a.sign, a.int, a.sig).asSInt
        fd.io.s_axis_a_tvalid := true.B
        fd.io.s_axis_b_tdata  := Cat(b.sign, b.int, b.sig).asSInt
        fd.io.s_axis_b_tvalid := true.B
        result.sign           := fd.io.m_axis_result_tdata(15)
        result.int            := fd.io.m_axis_result_tdata(14, 10)
        result.sig            := fd.io.m_axis_result_tdata(9, 0)
        result
    }
}

class FloatCmp extends BlackBox {
    val io = IO(new Bundle {
        val s_axis_a_tdata       = Input(SInt(16.W))
        val s_axis_a_tvalid      = Input(Bool())
        val s_axis_b_tdata       = Input(SInt(16.W))
        val s_axis_b_tvalid      = Input(Bool())
        val m_axis_result_tdata  = Output(UInt(8.W))
        val m_axis_result_tvalid = Output(Bool())
    })
}

object FloatCmp {
    def apply(a: FloatPoint, b: FloatPoint): Bool = {
        val result = Wire(Bool())
        val fc     = Module(new FloatCmp())
        fc.io.s_axis_a_tdata  := Cat(a.sign, a.int, a.sig).asSInt
        fc.io.s_axis_a_tvalid := true.B
        fc.io.s_axis_b_tdata  := Cat(b.sign, b.int, b.sig).asSInt
        fc.io.s_axis_b_tvalid := true.B
        fc.io.m_axis_result_tdata === 1.U
    }
}

class FloatRnd extends BlackBox {
    val io = IO(new Bundle {
        val s_axis_a_tdata       = Input(SInt(16.W))
        val s_axis_a_tvalid      = Input(Bool())
        val m_axis_result_tdata  = Output(UInt(16.W))
        val m_axis_result_tvalid = Output(Bool())
    })
}

object FloatRnd {
    def apply(a: FloatPoint): UInt = {
        val result = Wire(UInt(10.W))
        val fr     = Module(new FloatRnd())
        fr.io.s_axis_a_tdata  := Cat(a.sign, a.int, a.sig).asSInt
        fr.io.s_axis_a_tvalid := true.B
        result                := fr.io.m_axis_result_tdata
        result
    }
}
