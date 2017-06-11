package v1.bob

import javax.inject.{Inject, Provider}

import play.api.cache._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


class BobResourceHandler @Inject()(cache: CacheApi,
                                   routerProvider: Provider[BobRouter],
                                   pollService: PollService,
                                   bobRepo: BobRepository,
                                   pollRepo: PollRepository)
                                  (implicit ec: ExecutionContext) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def process(bobInput: BobFormInput): Future[SlackResponse] = {
    val command = bobInput.text.split(" ").toList
    command match {
      case "one" :: rest => getRandomOne
      case "list" :: rest => getList
      case "poll" :: rest => poll(bobInput.channelId)(rest)
      case "add" :: name => add(name.mkString(" "))
      case _ => Future.successful(SlackResponse("hi!"))
    }
  }

  private def add(name: String): Future[SlackResponse] = {
    bobRepo.add(name).map { id =>
      val distActions = List(
        IMActionBtn("dist", "Near", "near"),
        IMActionBtn("dist", "Mid", "mid"),
        IMActionBtn("dist", "Far", "far")
      )

      val categoryActions = List(
        IMActionBtn("category", "Korean", "korean"),
        IMActionBtn("category", "Chinese", "chinese"),
        IMActionBtn("category", "Japanese", "japanese"),
        IMActionBtn("category", "Etc", "etc")
      )
      val attachments = List(
        Attachment("distance", "test_fallback", s"bob_opt:$id:distance", distActions),
        Attachment("category", "test_fallback", s"bob_opt:$id:category", categoryActions)
      )

      SlackResponse("test", "in_channel", attachments)
    }
  }

  private def poll(channelId: String)(args: List[String]): Future[SlackResponse] = {
    val pick = if (args.nonEmpty) args.head.toInt else 4
    val list = Random.shuffle(bobRepo.list).take(pick)
    pollService.create(channelId, list)
  }

  def getList: Future[SlackResponse] = {
    bobRepo.getList.map { bobList =>
      SlackResponse(bobList.map(_.name).mkString(", "))
    }
  }

  private def getRandomOne: Future[SlackResponse] = {
    bobRepo.getList.map { bobDataList =>
      val pick = Random.shuffle(bobDataList).head
      SlackResponse(pick.name, "in_channel")
    }
  }

  def processReaction(payload: String): Future[SlackResponse] = {
    val json = Json.parse(payload)
    val userId = (json \ "user" \ "id").as[String]
    val interactType = (json \ "callback_id").as[String]
    val value = (json \ "actions" \\ "value").head.as[String]
    val name = (json \ "actions" \\ "name").head.as[String]

    val command: List[String] = interactType.split(":").toList

    command match {
      case "bob_poll" :: id :: rest =>
        pollService.vote(Vote(id.toLong, userId, value.toInt))
      case "bob_opt" :: id :: modType :: rest =>
        bobRepo.update(id.toLong)((modType, value)).map { _ =>
          SlackResponse("good")
        }
      case _ => Future.successful(SlackResponse("hi!"))
    }
  }
}
