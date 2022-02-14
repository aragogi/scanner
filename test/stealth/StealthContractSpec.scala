package stealth

import org.scalatest.matchers._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.propspec._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.util.encode.Base58
import utils.{RegexUtils, Utilities}
import scala.language.postfixOps
import stealth.StealthContract

class StealthContractSpec extends AnyPropSpec with TableDrivenPropertyChecks with should.Matchers with ScalaCheckPropertyChecks{

  private val stealthContract = new StealthContract()
  property("check stealth address") {
    val pk: String = "03b365599d8b7affb1e405d9aee464088135927b2c207055f8eb35e7da2bf4aa1a"
    val address: String = stealthContract.getStealthAddress(pk)
    val decodedAddress = Base58.decode(address).get
    import RegexUtils._
    val pattern = """[a-fA-F0-9]{2}(1004)((0e21)[a-fA-F0-9]{66}){4}(ceee7300ee7301ee7302ee7303)[a-fA-F0-9]{8}""".r
    assertResult(true) {
      pattern matches Utilities.toHexString(decodedAddress)
    }
  }

  property("check related stealth address") {
//    val stealthAddress: String = "6QBPS6hBLtb5PWfKpQReP4jRQ2ZBvnAW4KZ8FxHqxB5EADruPpcQXpV4UbQ1LEfjwC3DZDbAwemNJEC7JpNsVVE8jDY3y4FoKUdqCABgLzrLPAenYaSVgC2wjRD9svA24u3Gdbm7nSTr5mXdAbbYsKkYRN6xyZYgbZqrkZ4u2rPKx9jAa4oaAdzVm6nELoutAWbzybAumYGhreiYHyjFE44KQW"
//    val secret: String = "27493498882119732975929245578686427239090069644982650092437272045968198256307"
    val stealthAddress: String = "6QBPS6hEtpbkVWWcTpcumA8Jzx8U4sKC65WrRmwz4q72sr724e7W3fHB1JmDKNiJooJEF1Ker2WLvTbEQq8im33WDX73ryUUHg2h5kswrkxGwda2tV3h4DRcDB1WtMC2ETtiia4ovAewFKd5bvsSDhVxBUa3o4Sw1p4D6zKeY26hhPgTXnYA9gnWEoW38zym5XH9DJfGcAqCEwdqyeEqGryUzK"
    val secret: String = "00dba44461055a057b6889aa33e1f1167525a1e9064658ae14ff692c96b61a1d12"
      assertResult(true) {
        stealthContract.isSpendable(stealthAddress, secret)
      }
  }

  property("check unrelated stealth address") {
    val stealthAddress: String = "6QBPS6hGGHcXLBsmGVebkWtfWnSxLxG83kqXnaY9ukrrhZYVBu1UGweXUaNBD2Ekd67SHvjqMWcV8Lm8zB9j6Jr9XN8w24euGPxBzXWwijc5yhrUmY8evbtELQhZRS8goc8ZuMmzLJnhUHfofMABwXBq4QPDZkVTcmKCukMFWRjYY7mwGvLUCqNGiUgwbbiYVLuYHALzpK9LFHWLrVsUuCEauQ"
    val secret: String = "27493498882119732975929245578686427239090069644982650092437272045968198256307"
    assertResult(false) {
      stealthContract.isSpendable(stealthAddress, secret)
    }
  }
}
