package utils

import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoContract}
import org.scalatest.matchers._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.propspec._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.util.encode.Base64
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants.dlogGroup
import special.sigma.GroupElement
import utils.{NodeProcess, Utilities}

import scala.collection.immutable._
import scala.language.postfixOps

class NodeProcessSpec extends AnyPropSpec with TableDrivenPropertyChecks with should.Matchers with ScalaCheckPropertyChecks {
  val fakeStealthScript: String =
    s"""{
       |  val gr = decodePoint(fromBase64("GR"))
       |  val gy = decodePoint(fromBase64("GY"))
       |  val ur = decodePoint(fromBase64("UR"))
       |  val uy = decodePoint(fromBase64("UY"))
       |  proveDHTuple(gr,gy,ur,uy) && sigmaProp(OUTPUTS(0).R4[Int].get == 10)
       |}
       |""".stripMargin

  val stealthScript: String =
    s"""{
       |  val gr = decodePoint(fromBase64("GR"))
       |  val gy = decodePoint(fromBase64("GY"))
       |  val ur = decodePoint(fromBase64("UR"))
       |  val uy = decodePoint(fromBase64("UY"))
       |  proveDHTuple(gr,gy,ur,uy)
       |}
       |""".stripMargin


  def generateStealthContract(g: GroupElement, x: BigInt, script: String): ErgoContract = {
    val r = Utilities.randBigInt
    val y = Utilities.randBigInt
    val u: GroupElement = g.exp(x.bigInteger)

    val gr = Base64.encode(g.exp(r.bigInteger).getEncoded.toArray)
    val gy = Base64.encode(g.exp(y.bigInteger).getEncoded.toArray)
    val ur = Base64.encode(u.exp(r.bigInteger).getEncoded.toArray)
    val uy = Base64.encode(u.exp(y.bigInteger).getEncoded.toArray)

    Utilities.ergoClient.execute(ctx => {
      val newScript = script
        .replace("GR", gr)
        .replace("GY", gy)
        .replace("UR", ur)
        .replace("UY", uy)
      val contract = ctx.compileContract(
        ConstantsBuilder.create()
          .build()
        , newScript)
      val ergoTree = contract.getErgoTree
      println(Utilities.toHexString(ergoTree.bytes))
      contract
    })
  }

  def getStealthBoxes(value: Long, script: String): Seq[ErgoBox] = {
    val x = Utilities.randBigInt
    val g: GroupElement = dlogGroup.generator

    var outPuts = List()
    for (_ <- 0 until 5) {
      outPuts :+ Utilities.ergoClient.execute(ctx => {
        val txB = ctx.newTxBuilder()
        txB.outBoxBuilder()
          .value(value)
          .contract(generateStealthContract(g, x, script))
          .build()
      })
    }
    outPuts
  }


  property("check real stealth address") {
    val outPuts: Seq[ErgoBox] = getStealthBoxes(1000L, stealthScript)

    for (outPut <- outPuts) {
      assertResult(true) {
        NodeProcess.checkStealth(outPut)
      }
    }
  }

  property("check fake stealth address") {
    val outPuts: Seq[ErgoBox] = getStealthBoxes(1000L, fakeStealthScript)

    for (outPut <- outPuts) {
      NodeProcess.checkStealth(outPut) should be(false)
    }
  }
}
