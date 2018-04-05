package com.mpc.scalats

import org.scalatest.{FlatSpec, Matchers}

class CLISpec extends FlatSpec with Matchers {

  "The CLI" should "parse arguments" in {
    val cLIOpts = CLI.parseArgs(List("--only-packages","com.mpc, com.different","--emit-classes", "SomeClass"))

    cLIOpts.get(CLIOpts.EmitClasses) shouldBe Some(true)
    cLIOpts.get(CLIOpts.OnlyPackages) shouldBe Some(Array("com.mpc", "com.different"))
  }
}
