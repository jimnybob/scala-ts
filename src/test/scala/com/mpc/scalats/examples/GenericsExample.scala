package com.mpc.scalats.examples

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptGenerator

/**
 * Created by Milosz on 06.12.2016.
 */

trait Something

case class Foo[T, Q](a: T, b: List[Q])

case class Bar(b: Foo[String, String], c: List[Foo[Int, String]])

case class Xyz(bars: Option[List[Option[Bar]]], cache: Map[String,Something])

object GenericsExample {

  def main(args: Array[String]) {
    TypeScriptGenerator.generateFromClassNames(List(classOf[Xyz].getName), out = System.out)(Config())
  }

}
