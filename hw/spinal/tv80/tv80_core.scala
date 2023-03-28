package tv80

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.core.sim._

class tv80_core extends BlackBox{
    val io = new Bundle()
    {
        val reset_n = in Bool()            
        val clk = in Bool()                
        val cen = in Bool()                
        val wait_n = in Bool()             
        val int_n = in Bool()              
        val nmi_n = in Bool()              
        val busrq_n = in Bool()            
        val dinst = in Bits(8 bits)  
        val di = in Bits(8 bits)  

        val m1_n = out Bool()               
        val iorq = out Bool()               
        val no_read = out Bool()            
        val write = out Bool()              
        val rfsh_n = out Bool()             
        val halt_n = out Bool()             
        val busak_n = out Bool()            
        val A = out Bits(16 bits)   
        val data_o = out Bits(8 bits)      
        val mc = out Bits(7 bits)     
        val ts = out Bits(7 bits)      
        val intcycle_n = out Bool()     
        val IntE = out Bool()           
        val stop = out Bool()  
    }
    // Map the clk
    mapCurrentClockDomain(io.clk, io.reset_n, resetActiveLevel = LOW)

    // Remove io_ prefix
    noIoPrefix()
    addRTLPath("./hw/verilog/tv80_core.v")                         // Add a verilog file
    addRTLPath("./hw/verilog/tv80_mcode.v")                         // Add a verilog file
    addRTLPath("./hw/verilog/tv80_alu.v")                         // Add a verilog file
    addRTLPath("./hw/verilog/tv80_reg.v")                         // Add a verilog file
}

class tv80 extends Component
{
    val io = new Bundle()
    {               
        val cen = in Bool()                
        val wait_n = in Bool()             
        val int_n = in Bool()              
        val nmi_n = in Bool()              
        val busrq_n = in Bool()            
        val dinst = in Bits(8 bits)  
        val data_i = in Bits(8 bits)  

        val m1_n = out Bool()               
        val iorq = out Bool()               
        val no_read = out Bool()            
        val write = out Bool()              
        val rfsh_n = out Bool()             
        val halt_n = out Bool()             
        val busak_n = out Bool()            
        val A = out Bits(16 bits)   
        val data_o = out Bits(8 bits)      
        val mc = out Bits(7 bits)     
        val ts = out Bits(7 bits)      
        val intcycle_n = out Bool()     
        val IntE = out Bool()           
        val stop = out Bool()  
    }

    val tv80 = new tv80_core()
    io.cen <> tv80.io.cen
    io.wait_n <> tv80.io.wait_n
    io.int_n <> tv80.io.int_n
    io.nmi_n <> tv80.io.nmi_n
    io.busrq_n <> tv80.io.busrq_n
    io.dinst <> tv80.io.dinst
    io.m1_n <> tv80.io.m1_n
    io.iorq <> tv80.io.iorq
    io.no_read <> tv80.io.no_read
    io.write <> tv80.io.write
    io.rfsh_n <> tv80.io.rfsh_n
    io.halt_n <> tv80.io.halt_n
    io.busak_n <> tv80.io.busak_n
    io.A <> tv80.io.A
    io.data_o <> tv80.io.data_o
    io.mc <> tv80.io.mc
    io.ts <> tv80.io.ts
    io.intcycle_n <> tv80.io.intcycle_n
    io.IntE <> tv80.io.IntE
    io.stop <> tv80.io.stop 
    val di_reg = RegNextWhen(io.dinst, io.wait_n && tv80.io.ts(2)) init(0)
    tv80.io.di <> di_reg
}

object tv80_core_Sim {
	def main(args: Array[String]) {
		SimConfig.withFstWave.compile{
			val dut = new tv80()
			dut
		}.doSim{dut =>
			//Fork a process to generate the reset and the clock on the dut

			dut.clockDomain.forkStimulus(1)

			for(idx <- 0 to 300000){
				//Wait a rising edge on the clock
				dut.clockDomain.waitRisingEdge()
			}
		}
	}
}

object sim_tv80_Verilog extends App {
  Config_TV80.spinal.generateVerilog(new tv80())
}