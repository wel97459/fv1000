package tv80

import spinal.core._
import spinal.core.sim._

object Config_TV80 {
  def spinal = SpinalConfig(
    targetDirectory = "./simTV80/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = SYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
