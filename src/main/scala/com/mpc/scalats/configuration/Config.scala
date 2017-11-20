package com.mpc.scalats.configuration

/**
 * Created by Milosz on 09.12.2016.
 */
case class Config(
  emitInterfaces: Boolean = false,
  emitClasses: Boolean = true,
  optionToNullable: Boolean = true,
  optionToUndefined: Boolean = false
  )