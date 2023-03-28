package fv1000

import spinal.core._
import spinal.core.sim._

object Config_ECP5 {
  def spinal = SpinalConfig(
    targetDirectory = "./Colorlight_v8.0_ECP5/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = SYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}

object Config_fv1000 {
  def spinal = SpinalConfig(
    targetDirectory = "./simTV80/gen",
    device = Device.LATTICE,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = SYNC
    )
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}