package stealth

import org.ergoplatform.appkit.{Address, ConstantsBuilder}
import play.api.Logger
import scorex.util.encode.Base64
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants.dlogGroup
import special.sigma.GroupElement

import scala.language.postfixOps
import utils.Utilities


class StealthContract {
  private val logger: Logger = Logger(this.getClass)

  val stealthScript: String =
    s"""{
       |  val gr = decodePoint(fromBase64("GR"))
       |  val gy = decodePoint(fromBase64("GY"))
       |  val ur = decodePoint(fromBase64("UR"))
       |  val uy = decodePoint(fromBase64("UY"))
       |  proveDHTuple(gr,gy,ur,uy)
       |}
       |""".stripMargin

  def getStealthAddress(stealthPK: String): String = {
    val pk = getPK(stealthPK)

    val g: GroupElement = dlogGroup.generator
    val r = Utilities.randBigInt
    val y = Utilities.randBigInt
    val u: GroupElement = Utilities.hexToGroupElement(pk)

    val gr = Base64.encode(g.exp(r.bigInteger).getEncoded.toArray)
    val gy = Base64.encode(g.exp(y.bigInteger).getEncoded.toArray)
    val ur = Base64.encode(u.exp(r.bigInteger).getEncoded.toArray)
    val uy = Base64.encode(u.exp(y.bigInteger).getEncoded.toArray)

    Utilities.ergoClient.execute(ctx => {
      val newScript = stealthScript
        .replace("GR", gr)
        .replace("GY", gy)
        .replace("UR", ur)
        .replace("UY", uy)
      val contract = ctx.compileContract(
        ConstantsBuilder.create()
          .build()
        , newScript)
      val ergoTree = contract.getErgoTree
      val stealthAddress = Utilities.addressEncoder.fromProposition(ergoTree).get.toString
      stealthAddress
    })
  }

  def isSpendable(address: String, secret: String): Boolean = {
    val stealthAddress = Address.create(address)
    val x = BigInt(secret, 16)

    val hexErgoTree = Utilities.toHexString(stealthAddress.getErgoAddress.script.bytes)
    val gr = Utilities.hexToGroupElement(hexErgoTree.slice(8, 74))
    val gy = Utilities.hexToGroupElement(hexErgoTree.slice(78, 144))
    val ur = Utilities.hexToGroupElement(hexErgoTree.slice(148, 214))
    val uy = Utilities.hexToGroupElement(hexErgoTree.slice(218, 284))

    try {
      if (gr.exp(x.bigInteger) == ur && gy.exp(x.bigInteger) == uy) {
        true
      }
      else false
    }
    catch {
      case e: Exception => logger.error(e.getMessage)
        false
    }
  }

  def getPK(pk: String): String = {
    val stealthArray = pk.split(":")
    if (stealthArray(0) != "stealth") {
      logger.error("wrong format pk")
      throw new Throwable("wrong format pk")
    }
    stealthArray(1)
  }
}
