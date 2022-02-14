package controllers

import akka.actor.ActorSystem
import dao._
import io.circe.{Encoder, Json}
import io.circe.parser.{parse => circeParse}
import io.circe.syntax._

import javax.inject._
import org.ergoplatform.wallet.serialization.JsonCodecsWrapper
import org.ergoplatform.ErgoBox
import play.api.Logger
import play.api.mvc._
import utils.ErrorHandler._
import utils.NodeProcess.lastHeight

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import stealth.StealthContract

@Singleton
class Controller @Inject()(extractedBlockDAO: ExtractedBlockDAO, stealthContract: StealthContract, outputDAO: OutputDAO, cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Sample controller
   */
  def home: Action[AnyContent] = Action { implicit request =>
    logger.info("First Api!")
    Ok("ok")
  }

  /**
   * List boxes which are unSpent for spec scanId. Route: /scan/unspentBoxes/{scanId}
   */
  def listUBoxes(minConfirmations: Int, minInclusionHeight: Int): Action[AnyContent] = Action { implicit request =>
    try {
      val scans = outputDAO.selectUnspentBoxesWithScanId(1, lastHeight - minConfirmations, minInclusionHeight)
      implicit val boxDecoder: Encoder[ErgoBox] = JsonCodecsWrapper.ergoBoxEncoder
      Ok(scans.asJson.toString()).as("application/json")
    }
    catch {
      case e: Exception => errorResponse(e)
    }
  }

  /**
   * status of scanner. Route: /info
   *
   * @return {
   *         "lastScannedHeight": 563885,
   *         "networkHeight": 573719
   *         }
   */
  def scanInfo: Action[AnyContent] = Action { implicit request =>
    try {
      val lastScannedHeight = ("lastScannedHeight", Json.fromInt(extractedBlockDAO.getLastHeight))
      val networkHeight = ("networkHeight", Json.fromInt(lastHeight))
      Ok(Json.fromFields(List(lastScannedHeight, networkHeight)).toString()).as("application/json")
    }
    catch {
      case e: Exception => errorResponse(e)
    }
  }

  def generateStealthAddress(): Action[JsValue] = Action(parse.json) { implicit request =>
    try {
      var result: Json = Json.Null
      circeParse(request.body.toString).toTry match {
        case Success(stealthJs) =>
          val stealthPK = stealthJs.hcursor.downField("stealthPK").as[String].getOrElse(throw new Exception("stealthPK is required"))
          result = Json.fromFields(List(("address", Json.fromString(stealthContract.getStealthAddress(stealthPK)))))

        case Failure(e) => throw new Exception(e)
      }
      Ok(result.toString()).as("application/json")
    }
    catch {
      case m: NotFoundException => notFoundResponse(m.getMessage)
      case e: Exception => errorResponse(e)
    }
  }

  def isSpendable: Action[JsValue] = Action(parse.json) { implicit request =>
    try {
      var result: Json = Json.Null
      circeParse(request.body.toString).toTry match {
        case Success(stealthJs) =>
          val address = stealthJs.hcursor.downField("address").as[String].getOrElse(throw new Exception("address is required"))
          val secret = stealthJs.hcursor.downField("secret").as[String].getOrElse(throw new Exception("secret is required"))
          result = Json.fromFields(List(("status", Json.fromBoolean(stealthContract.isSpendable(address, secret)))))
        case Failure(e) => throw new Exception(e)
      }
      Ok(result.toString()).as("application/json")
    }
    catch {
      case m: NotFoundException => notFoundResponse(m.getMessage)
      case e: Exception => errorResponse(e)
    }
  }

}
