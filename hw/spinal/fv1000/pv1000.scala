package testing

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.core.sim._

class pv1000 extends Component{
    val io = new Bundle(){
        val HSync_ = out Bool()
        val VSync_ = out Bool()
        val CompSync_ = out Bool()
        val Pixel = out Bool()
        val Burst = out Bool()
        val AddSTB_ = out Bool()
    }
    //Registers
        val VerticalCounter = Reg(UInt(9 bits)) init(0)
        val HorizontalCounter = Reg(UInt(7 bits)) init(70)
        val TimingCounter = Reg(UInt(4 bits)) init(0)
        val PixelShifter = Reg(Bits(8 bits))

    //Signals
        val VSync_NTSC = VerticalCounter >= 258 && VerticalCounter <= 262
        val VSync = VSync_NTSC

        val VDisplay_NTSC = VerticalCounter >= 36 && VerticalCounter < 227
        val VDisplay = VDisplay_NTSC

        val VPreDisplay_NTSC = VerticalCounter >= 35 && VerticalCounter < 227
        val VPreDisplay = VPreDisplay_NTSC

        val VReset_NTSC = VerticalCounter === 262
        val VReset = VReset_NTSC

        val HSync = HorizontalCounter >= 66 && HorizontalCounter <= 70
        val Burst = HorizontalCounter >= 1 && HorizontalCounter <= 5

        val VerticalBlanking = VerticalCounter <= 10 || VerticalCounter >= 252

        val HDisplay = HorizontalCounter >= 6 && HorizontalCounter <= 61

        val DotClk8 = TimingCounter === 0
        val DotClk4 = TimingCounter === 3 || TimingCounter === 7

        val AddSTB_ = (HDisplay && VDisplay) ? !DotClk8 | True

        when(TimingCounter === 7){
            TimingCounter := 0
        }otherwise{
            TimingCounter := TimingCounter + 1
        }

        when(DotClk4){
            when(HorizontalCounter === 70){
                HorizontalCounter := 0
                TimingCounter := 0
                when(VReset){
                    VerticalCounter := 0
                }otherwise{
                    VerticalCounter := VerticalCounter + 1
                }
            }otherwise{
                HorizontalCounter := HorizontalCounter + 1
            }
        }

        val vcOff = VerticalCounter(2 downto 0) + 4;

        val d = B"00000000"
        when(vcOff === 0){
            d := B"00111000"
        }elsewhen(vcOff === 1){
            d := B"01001100"
        }elsewhen(vcOff === 2){
            d := B"11000110"
        }elsewhen(vcOff === 3){
            d := B"11000110"
        }elsewhen(vcOff === 4){
            d := B"11000110"
        }elsewhen(vcOff === 5){
            d := B"01100100"
        }elsewhen(vcOff === 6){
            d := B"00111000"
        }elsewhen(vcOff === 7){
            d := B"00000000"
        }

        when(!AddSTB_){
            PixelShifter := d
        }otherwise{
            PixelShifter := PixelShifter |<< 1
        }

    //Outputs
        io.Pixel := PixelShifter(7)
        io.Burst := Burst & !VerticalBlanking
        io.CompSync_ := !(HSync ^ VSync)
        io.HSync_ := !HSync
        io.VSync_ := !VSync

        io.AddSTB_ := AddSTB_

        val c = Reg(UInt(4 bits))
        when(c===4){ 
            c := 0
        }otherwise{
            c := c + 1
        }

        val dd = U"0010" + c
        val dm = (dd > 4) ? (dd-5) | dd
}

object pv1000_Sim {
	def main(args: Array[String]) {
		SimConfig.withFstWave.compile{
			val dut = new pv1000()
			dut
		}.doSim{dut =>
			//Fork a process to generate the reset and the clock on the dut

			dut.clockDomain.forkStimulus(period = 223492)

			for(idx <- 0 to 300000){
				//Wait a rising edge on the clock
				dut.clockDomain.waitRisingEdge()
			}
		}
	}
}