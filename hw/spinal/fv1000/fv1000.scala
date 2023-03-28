package fv1000

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.core.sim._

class fv1000 extends Component {
    val io = new Bundle(){
        val addr = out Bits(16 bits)   
        val data_o = out Bits(8 bits) 
        val data_i = in Bits(8 bits)

        val CompSync_ = out Bool()
        val Pixel = out Bool()
        val Burst = out Bool()
        val VSync_ = out Bool()
        val Video = in UInt(8 bits)
    }
    io.addr := 0
    io.data_o := 0
    val gfx = new pv1000_gfx()
    io.CompSync_ := gfx.io.CompSync_
    io.Burst := gfx.io.Burst
    io.Pixel := gfx.io.Pixel
    io.VSync_ := gfx.io.VSync_
}


object sim_fv1000_Verilog extends App {
  Config_fv1000.spinal.generateVerilog(new fv1000())
}