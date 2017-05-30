package v1.bob

import javax.inject.{Inject, Provider}

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

case class BobPoll(poll: Poll, candidates: Seq[Candidate], votes: Seq[Vote]) {
  val id: Long = poll.id
  val isOpen: Boolean = poll.isOpen
  val candidatesOptional: Seq[Candidate] =
    if (isOpen) candidates :+ Candidate(poll.id, -1, -1, "Close", "danger") else candidates
  val method: String = if (poll.messageTs.isEmpty) "chat.postMessage" else "chat.update"
  val result: Map[Int, Set[Vote]] =
    votes
      .groupBy(identity)
      .mapValues(_.length)
      .filter(_._2 % 2 == 1)
      .keySet
      .groupBy(_.selection)
}

object BobPoll {
  implicit val implicitWrites = new Writes[BobPoll] {
    override def writes(o: BobPoll): JsValue = {
      val buttons = Json.obj(
        "fallback" -> "Vote Failed!",
        "callback_id" -> s"bob_poll:${o.id}",
        "color" -> "#3AA3E3",
        "attachment_type" -> "default",
        "actions" -> o.candidatesOptional
      )
      val voters = {
        val votedUsers = o.result.flatMap(_._2).map(_.userMentionStr).toSet
        val votedUserStr = if (votedUsers.isEmpty) "Nobody" else votedUsers.mkString(", ")
        Json.obj(
          "text" -> s"$votedUserStr voted."
        )
      }
      Json.toJson(
        Json.arr(buttons, voters)
      )
    }
  }
}

class PollService @Inject()(routerProvider: Provider[BobRouter],
                            slack: SlackController,
                            bobRepo: BobRepository,
                            pollRepo: PollRepository,
                            candidateRepo: CandidateRepository,
                            voteRepo: VoteRepository)
                           (implicit ec: ExecutionContext){

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def createPoll(bobList: Seq[Bob]): BobPoll = {
    val pollId = Await.result(pollRepo.createNewPoll(), 10 seconds)
    val candidates =
      bobList
        .zip(Stream from 1)
        .map { case (bob, index) => Candidate(pollId, index, bob.id, bob.name) }
    Await.result(candidateRepo.insert(candidates), 10 seconds)
    getPoll(pollId)
  }

  def getPoll(pollId: Long): BobPoll = {
    val poll = Await.result(pollRepo.select(pollId), 10 seconds)
    val candidates = Await.result(candidateRepo.select(pollId), 10 seconds)
    val votes = Await.result(voteRepo.select(pollId), 10 seconds)

    BobPoll(poll, candidates, votes)
  }

  def vote(v: Vote): String = {
    Await.result(voteRepo.vote(v), 10 seconds)
    val result = Await.result(pollRepo.getPollResult(v.pollId), 10 seconds)
    val resultStr = {
      val resultSet =
        result
          .filter(_.userId == v.userId)
          .map(_.candidateName)

      if (resultSet.isEmpty) "*Nowhere*" else resultSet.mkString("'", "', '", "'")
    }
    val poll = getPoll(v.pollId)
    postPoll(poll)
    s"You voted to $resultStr"
  }

  def close(pollId: Long): String = {
    Await.result(pollRepo.closePoll(pollId), 10 seconds)
    val poll = getPoll(pollId)
    val result = poll.result
    val bob: Map[Long, String] = Await.result( bobRepo.list.map { list =>
      list.map(b => b.id -> b.name).toMap
    }, 10 seconds)
    val candidates = poll.candidates.map(c => c.serialNo -> bob.getOrElse(c.bobId, c.serialNo.toString)).toMap
    val resultStr =
      if (result.isEmpty) "Nothing Selected"
      else poll
        .result
        .mapValues(_.map(_.userMentionStr))
        .map(a => s"${candidates.getOrElse(a._1, a._1.toString)}: ${a._2.size} - ${a._2.mkString(", ")}")
        .mkString("\n")
    postPoll(poll)

    s"""
       |*Poll Closed!*
       |$resultStr
     """.stripMargin
  }

  def postPoll(poll: BobPoll): Future[Int] = {
    val conf = ConfigFactory.load
    val postBody =
      Map(
        "token" -> Seq(conf.getString("slack.client.token")),
        "channel" -> Seq(conf.getString("slack.client.channel")),
        "ts" -> Seq(poll.poll.messageTs.getOrElse("")),
        "text" -> Seq("Lunch!"),
        "attachments" -> Seq(Json.stringify(Json.toJson(poll)))
      )
    val messageTs = slack.postMessage(poll.method, postBody)
    pollRepo.updateMessageTs(poll.id)(messageTs)
  }
}
