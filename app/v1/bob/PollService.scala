package v1.bob

import javax.inject.{Inject, Provider}

import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PollService @Inject()(routerProvider: Provider[BobRouter],
                            slack: SlackController,
                            bobRepo: BobRepository,
                            pollRepo: PollRepository,
                            candidateRepo: CandidateRepository,
                            voteRepo: VoteRepository)
                           (implicit ec: ExecutionContext) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def create(bobList: Seq[Bob]): Future[SlackSimpleResponse] = {
    createPoll(bobList).flatMap(identity).map { poll =>
      postPoll(poll)
      SlackSimpleResponse("Poll Created")
    }
  }

  private def createPoll(bobList: Seq[Bob]): Future[Future[BobPoll]] = {
    pollRepo.createNewPoll().map { pollId =>
      val candidates =
        bobList
          .zip(Stream from 1)
          .map { case (bob, index) => Candidate(pollId, index, bob.id, bob.name) }

      candidateRepo.insert(candidates).map(i => getPoll(pollId))
    }
  }

  def getPoll(pollId: Long): BobPoll = {
    val poll = Await.result(pollRepo.select(pollId), 10 seconds)
    val candidates = Await.result(candidateRepo.select(pollId), 10 seconds)
    val votes = Await.result(voteRepo.select(pollId), 10 seconds)

    BobPoll(poll, candidates, votes)
  }

  private def doVote(v: Vote): Future[BobPoll] = {
    def func = v.selection match {
      case -1 => pollRepo.closePoll(v.pollId)
      case _ => voteRepo.vote(v)
    }

    func.map { i =>
      Await.result(postPoll(getPoll(v.pollId)), 10 seconds)
    }
  }

  def vote(v: Vote): Future[SlackSimpleResponse] = {
    doVote(v).map { poll =>
      val bob = bobRepo.asMap
      val resultStr = poll.resultByUser.getOrElse(v.userId, Nil).map { vote =>
        bob.getOrElse(poll.candidateBobIdMap(vote.selection), vote.selection.toString)
      }.mkString(", ")
      SlackSimpleResponse(s"You voted to $resultStr")
    }
  }

  def close(v: Vote): Future[SlackSimpleResponse] = {
    doVote(v).map { poll =>
      val result = poll.resultBySelection
      val bob: Map[Long, String] = bobRepo.asMap
      val resultStr =
        if (result.isEmpty) "Nothing Selected"
        else
          result
            .mapValues(_.map(_.userMentionStr))
            .map(a => s"${bob.getOrElse(poll.candidateBobIdMap(a._1), a._1.toString)}: ${a._2.size} - ${a._2.mkString(", ")}")
            .mkString("\n")

      SlackSimpleResponse(s"*Poll Closed!*\n$resultStr", "in_channel")
    }
  }

  private def postPoll(poll: BobPoll): Future[BobPoll] = {
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
    pollRepo.updateMessageTs(poll.id)(messageTs).map { i =>
      poll
    }
  }
}
