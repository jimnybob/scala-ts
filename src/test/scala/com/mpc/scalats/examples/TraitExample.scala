package com.mpc.scalats.examples

import com.different.pack.DontInclude
import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptGenerator

trait Pet {
  val name: String
}

case class Cat(name: String) extends Pet with DontInclude

case class Dog(name: String, weight: BigDecimal) extends Pet

case class Zoo(pets: Seq[Pet])

object TraitExample {

  def main(args: Array[String]) {
    TypeScriptGenerator.generateFromClassNames(
      List(
        classOf[Zoo].getName,
        classOf[Cat].getName,
        classOf[Dog].getName
      ),
      out = System.out)(Config(onlyPackages = Seq("com.mpc")))
  }

}
