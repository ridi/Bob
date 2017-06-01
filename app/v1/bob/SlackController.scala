package v1.bob

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class SlackController @Inject()(ws: WSClient)(implicit ec: ExecutionContext) {
  def postMessage(method: String, body: Map[String, Seq[String]]): String = {
    val conf = ConfigFactory.load
    Await.result(
      ws.url(conf.getString("slack.url") + method)
        .withHeaders("content-type" -> "application/x-www-form-urlencoded")
        .post(body)
        .map { response =>
          (response.json \ "ts").as[String]
        }, 10 seconds)
  }

}

case class SlackSimpleResponse(text: String, responseType: String = "ephemeral", replace: Boolean = false)

object SlackSimpleResponse {
  implicit val implicitWrites = new Writes[SlackSimpleResponse] {
    def writes(message: SlackSimpleResponse): JsValue = {
      Json.obj(
        "response_type" -> message.responseType,
        "replace_original" -> message.replace,
        "text" -> message.text
      )
    }
  }
}