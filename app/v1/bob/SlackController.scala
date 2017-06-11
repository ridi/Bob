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

case class IMActionBtn(name: String,
                       text: String,
                       value: String)

object IMActionBtn {
  implicit val implicitWrites = new Writes[IMActionBtn] {
    def writes(action: IMActionBtn): JsValue = {
      Json.obj(
        "name" -> action.name,
        "text" -> action.text,
        "type" -> "button",
        "value" -> action.value
      )
    }
  }
}

case class Attachment(title: String,
                      fallback: String,
                      callbackId: String,
                      actions: List[IMActionBtn])


object Attachment {
  implicit val implicitWrites = new Writes[Attachment] {
    def writes(attachment: Attachment): JsValue = {
      Json.obj(
        "title" -> attachment.title,
        "fallback" -> attachment.fallback,
        "callback_id" -> attachment.callbackId,
        "actions" -> attachment.actions
      )
    }
  }
}

case class SlackResponse(text: String,
                         responseType: String = "ephemeral",
                         attachments: List[Attachment] = Nil,
                         replace: Boolean = false)

object SlackResponse {
  implicit val implicitWrites = new Writes[SlackResponse] {
    def writes(message: SlackResponse): JsValue = {
      Json.obj(
        "response_type" -> message.responseType,
        "replace_original" -> message.replace,
        "attachments" -> message.attachments,
        "text" -> message.text
      )
    }
  }
}
