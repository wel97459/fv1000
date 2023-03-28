package testing

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40._
import MySpinalHardware._

class Top_pv1000 extends Component{
    val io = new Bundle{
        val reset_ = in Bool()
        val clk_25Mhz = in Bool() //12Mhz CLK
        val clk17 = in Bool()
        val phyrst_ = out Bool()
        val video = out Bits(2 bits)
        val sync = out Bool()
        val burst = inout(Analog(Bits(3 bits)))

        val scl = inout(Analog(Bool()))
        val sda = inout(Analog(Bool()))

        val TapeIn = in Bool()
        val TapeTest = out Bool()
        val Dial = in Bits(4 bits)
        //val pwm_sound = out Bool()
        val led_red = out Bool()
    }
    noIoPrefix()
    io.phyrst_ := True

    val clk18Domain = ClockDomain.internal(name = "Core18",  frequency = FixedFrequency(17.897727 MHz))
    val clk25Domain = ClockDomain.internal(name = "Core25",  frequency = FixedFrequency(25.0000 MHz))

    clk25Domain.clock := io.clk_25Mhz
    clk25Domain.reset := !io.reset_

    clk18Domain.clock := io.clk17

    val Core18 = new ClockingArea(clk18Domain) {
        val areaDiv4 = new SlowArea(4) {
            val pv = new pv1000()
            io.sync := pv.io.CompSync_
            io.video := pv.io.Pixel ? B"10" | B"00"
        }

        val c = Reg(UInt(4 bits))
        when(c===4){ 
            c := 0
        }otherwise{
            c := c + 1
        }
        val BurstOut = B"000"
        val Burst = io.burst
        when(areaDiv4.pv.io.Burst || areaDiv4.pv.io.Pixel)
        {
            io.burst := BurstOut
        }

        when(areaDiv4.pv.io.Burst || (areaDiv4.pv.io.Pixel && io.Dial.asUInt === 0))
        {
            when(c === 0){
                BurstOut := B"3'h7"
            }elsewhen(c === 1){
                BurstOut := B"3'h2"
            }elsewhen(c === 2){
                BurstOut := B"3'h0"
            }elsewhen(c === 3){
                BurstOut := B"3'h5"
            }elsewhen(c === 4){
                BurstOut := B"3'h7"
            }
        }elsewhen(areaDiv4.pv.io.Pixel && io.Dial.asUInt === 1){
            when(c === 0){
                BurstOut := B"3'h7"
            }elsewhen(c === 1){
                BurstOut := B"3'h7"
            }elsewhen(c === 2){
                BurstOut := B"3'h2"
            }elsewhen(c === 3){
                BurstOut := B"3'h0"
            }elsewhen(c === 4){
                BurstOut := B"3'h5"
            }
        }elsewhen(areaDiv4.pv.io.Pixel && io.Dial.asUInt === 2){
            when(c === 0){
                BurstOut := B"3'h5"
            }elsewhen(c === 1){
                BurstOut := B"3'h7"
            }elsewhen(c === 2){
                BurstOut := B"3'h7"
            }elsewhen(c === 3){
                BurstOut := B"3'h2"
            }elsewhen(c === 4){
                BurstOut := B"3'h0"
            }
        }elsewhen(areaDiv4.pv.io.Pixel && io.Dial.asUInt === 3){
            when(c === 0){
                BurstOut := B"3'h0"
            }elsewhen(c === 1){
                BurstOut := B"3'h5"
            }elsewhen(c === 2){
                BurstOut := B"3'h7"
            }elsewhen(c === 3){
                BurstOut := B"3'h7"
            }elsewhen(c === 4){
                BurstOut := B"3'h2"
            }
        }elsewhen(areaDiv4.pv.io.Pixel && io.Dial.asUInt === 4){
            when(c === 0){
                BurstOut := 2
            }elsewhen(c === 1){
                BurstOut := 0
            }elsewhen(c === 2){
                BurstOut := 5
            }elsewhen(c === 3){
                BurstOut := 7
            }elsewhen(c === 4){
                BurstOut := 7
            }
        }

        
        val pwm = new LedGlow(25)
        io.TapeTest := !(pwm.io.led)
    }

    val Core25 = new ClockingArea(clk25Domain) {
        val area40kHz = new SlowArea(50 kHz, true) {
            val SkipOSC = Reg(Bool()) init(False)
            val si = new Si5351("./data/si5351_17.897727.bin")
            si.io.i_scl := io.scl
            si.io.i_sda := io.sda
            si.io.i_skip := False
        }

        when(!area40kHz.si.io.o_sda_write){
            io.sda := False
        }
        when(!area40kHz.si.io.o_scl_write){
            io.scl := False
        } 

        val pwm = new LedGlow(25)
        io.led_red := !(pwm.io.led || area40kHz.si.io.o_done)
    }
    clk18Domain.reset := !io.reset_
}


object Top_pv1000_ECP5_Verilog extends App {
  Config_ECP5.spinal.generateVerilog(new Top_pv1000())
}