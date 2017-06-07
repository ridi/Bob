package v1.bob

import javax.inject.{Inject, Provider}

import play.api.cache._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}


class BobResourceHandler @Inject()(cache: CacheApi,
                                   routerProvider: Provider[BobRouter],
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
      case "poll" :: rest => poll(bobInput.channelId)(rest)
      case "add" :: name :: rest => add(name)
      case _ => Future.successful(SlackSimpleResponse("hi!"))
    }
  }

  private def add(name: String): Future[SlackSimpleResponse] = {
    bobRepo.add(name).map { id =>
      SlackSimpleResponse(s"$name added")
    }
  }

  private def poll(channelId: String)(args: List[String]): Future[SlackSimpleResponse] = {
    val pick = if (args.nonEmpty) args.head.toInt else 4
    val list = Random.shuffle(bobRepo.list).take(pick)
    pollService.create(channelId, list)
  }

  def getList: Future[SlackSimpleResponse] = {
    bobRepo.getList.map { bobList =>
      SlackSimpleResponse(bobList.map(_.name).mkString(", "))
    }
  }

  private def getRandomOne: Future[SlackSimpleResponse] = {
    bobRepo.getList.map { bobDataList =>
      val pick = Random.shuffle(bobDataList).head
      SlackSimpleResponse(pick.name, "in_channel")
    }
  }

  def processReaction(payload: String): Future[SlackSimpleResponse] = {
    val json = Json.parse(payload)
    val selection = (json \ "actions" \\ "value").head.as[String].toInt
    val pollId = (json \ "actions" \\ "name").head.as[String].toLong
    val userId = (json \ "user" \ "id").as[String]

    val vote = Vote(pollId, userId, selection)
    reactValidation(vote) match {
      case Success(_) =>
        pollService.vote(vote)
      case Failure(_) =>
        Future.successful(
          SlackSimpleResponse("poll closed!")
        )
    }
  }

  private def reactValidation(v: Vote): Try[Unit] =
    Try {
      require(Await.result(pollRepo.checkPollOpened(v.pollId), 5 seconds))
    }
}
