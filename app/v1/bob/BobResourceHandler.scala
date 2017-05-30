package v1.bob

import javax.inject.{Inject, Provider}

import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


class BobResourceHandler @Inject()(routerProvider: Provider[BobRouter],
                                   pollService: PollService,
                                   bobRepo: BobRepository,
                                   pollRepo: PollRepository)
                                  (implicit ec: ExecutionContext) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)


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

  private def add(name: String): Future[SlackSimpleResponse] = {
    bobRepo.add(name).map { id =>
      createSimpleResponse(s"$name added")
    }
  }

  private def poll(args: List[String]): Future[SlackSimpleResponse] = {
    val pick = if (args.nonEmpty) args.head.toInt else 4
    bobRepo.list.map { list =>
      val bobList = Random.shuffle(list).take(pick)
      val poll = pollService.createPoll(bobList)
      pollService.postPoll(poll)

      createSimpleResponse("poll created")
    }
  }

  def getList: Future[SlackSimpleResponse] = {
    bobRepo.list.map { bobList =>
      createSimpleResponse(bobList.map(_.name).mkString(", "))
    }
  }

  private def getRandomOne: Future[SlackSimpleResponse] = {
    bobRepo.list.map { bobDataList =>
      val pick = Random.shuffle(bobDataList).head
      createSimpleResponse(pick.name, "in_channel")
    }
  }

  def processReaction(payload: String): Future[SlackSimpleResponse] = {
    val json = Json.parse(payload)
    val selection = (json \ "actions" \\ "value").head.as[String].toInt
    val pollId = (json \ "actions" \\ "name").head.as[String].toLong
    val userId = (json \ "user" \ "id").as[String]

    val vote = Vote(pollId, userId, selection)
    val poll = pollService.getPoll(pollId)

    val result =
      if (poll.isOpen) {
        selection match {
          case -1 => (pollService.close(vote.pollId), "in_channel")
          case _ => (pollService.vote(vote), "ephemeral")
        }
      } else ("Poll closed!", "in_channel")

    Future.successful(createSimpleResponse(result._1, result._2))
  }

  private def createSimpleResponse(
                                    text: String,
                                    responseType: String = "ephemeral",
                                    replace: Boolean = false): SlackSimpleResponse =
    SlackSimpleResponse(text, responseType, replace)
}