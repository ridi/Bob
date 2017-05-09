package v1.bob

import javax.inject.{Inject, Provider}

import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class SlackSimpleResponse(text: String)

case class Actions(name: String, text: String, `type`: String, value: String)

case class Attachments(text: String,
                       fallback: String,
                       callback_id: String,
                       color: String,
                       attachmentType: String,
                       actions: List[Actions])

object SlackSimpleResponse {
  implicit val implicitWrites = new Writes[SlackSimpleResponse] {
    def writes(bob: SlackSimpleResponse): JsValue = {
      Json.obj("text" -> bob.text)
    }
  }
}

class BobResourceHandler @Inject()(routerProvider: Provider[BobRouter],
                                   bobRepository: BobRepository)
                                  (implicit ec: ExecutionContext) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def add(name: String): Future[SlackSimpleResponse] = {
    bobRepository.add(name).map { id =>
      createSimpleResponse(s"$name added")
    }
  }

  def process(bobInput: BobFormInput): Future[SlackSimpleResponse] = {
    val command = bobInput.text.split(" ").toList
    command match {
      case "one" :: rest => getRandomOne
      case "list" :: rest => getList
      case "poll" :: rest => poll(rest)
      case "add" :: name :: rest => add(name)
      case _ => Future.successful(createSimpleResponse("hi!"))
    }
  }

  def poll(args: List[String]): Future[SlackSimpleResponse] = {
    val pick = if (args.nonEmpty) args.head.toInt else 4
    bobRepository.list.map { list =>
      val candidates = Random.shuffle(list).take(pick)
      createSimpleResponse(candidates.map(_.name).mkString(", "))
    }
  }

  def getList: Future[SlackSimpleResponse] = {
    bobRepository.list.map { bobList =>
      createSimpleResponse(bobList.map(_.name).mkString(", "))
    }
  }

  private def getRandomOne: Future[SlackSimpleResponse] = {
    bobRepository.list.map { bobDataList =>
      val pick = Random.shuffle(bobDataList).head
      createSimpleResponse(pick.name)
    }
  }

  def lookup(id: Int): Future[Option[SlackSimpleResponse]] = {
    val bobFuture = bobRepository.getOne(id)
    bobFuture.map { maybeBobData =>
      maybeBobData.map { bob=>
        createSimpleResponse(bob.name)
      }
    }
  }

  private def createSimpleResponse(text: String): SlackSimpleResponse = SlackSimpleResponse(text)
}