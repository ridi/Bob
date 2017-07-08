package v1.bob

import javax.inject.{Inject, Provider}

import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

class PollService @Inject()(routerProvider: Provider[BobRouter],
                            slack: SlackController,
                            bobRepo: BobRepository,
                            pollRepo: PollRepository,
                            candidateRepo: CandidateRepository,
                            pollResultRepo: PollResultRepository,
                            voteRepo: VoteRepository)
                           (implicit ec: ExecutionContext) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def create(channelId: String, bobList: Seq[Bob]): Future[Future[BobPoll]] = {
    createPoll(channelId, bobList).flatMap(identity).map { poll =>
      postPoll(poll)
    }
  }

  private def createPoll(channelId: String, bobList: Seq[Bob]): Future[Future[BobPoll]] = {
    pollRepo.createNewPoll(channelId).map { pollId =>
      val candidates =
        bobList
          .zip(Stream from 1)
          .map { case (bob, index) => Candidate(pollId, index, bob.id, bob.name) }

      candidateRepo.insert(candidates).map(i => getPoll(pollId))
    }
  }

  private def getPoll(pollId: Long): BobPoll = {
    val poll = Await.result(pollRepo.select(pollId), 10 seconds)
    val candidates = Await.result(candidateRepo.select(pollId), 10 seconds)
    val votes = Await.result(voteRepo.select(pollId), 10 seconds)

    BobPoll(poll, candidates, votes)
  }

  def vote(v: Vote): Future[List[String]] = {
    Await.result(voteRepo.vote(v), 10 seconds)
    val poll = getPoll(v.pollId)

    postPoll(poll).map{ _ =>
      poll
        .resultByUser
        .getOrElse(v.userId, Nil)
        .map(vote => poll.candidateInfo(v.selection)._2)
        .toList
    }
  }

  def close(pollId: Long): Future[List[String]] = {
    Await.result(pollRepo.closePoll(pollId), 10 seconds)
    val poll = getPoll(pollId)
    val pollResult = poll.resultBySelection
    val maxVoteCount = pollResult.mapValues(_.size).toSeq.maxBy(_._2)._2
    val selections = pollResult.mapValues(_.size).filter(_._2 == maxVoteCount).keySet
    val selection = Random.shuffle(selections).head

    Await.result(pollResultRepo.insert(PollResult(pollId, poll.candidateInfo(selection)._1)), 10 seconds)
    postPoll(poll).map { _ =>
      pollResult
        .mapValues(_.map(_.userMentionStr))
        .map { case (bobId, voters) =>
          if (bobId == selection)
            s":tada: *${poll.candidateInfo(bobId)._2}: ${voters.size} - ${voters.mkString(", ")}*"
          else
            s"${poll.candidateInfo(bobId)._2}: ${voters.size} - ${voters.mkString(", ")}"
        }
        .toList
    }
  }

  private def postPoll(poll: BobPoll): Future[BobPoll] = {
    val conf = ConfigFactory.load
    val postBody =
      Map(
        "token" -> Seq(conf.getString("slack.client.token")),
        "channel" -> Seq(poll.channel),
        "ts" -> Seq(poll.messageTs),
        "text" -> Seq("Lunch!"),
        "attachments" -> Seq(Json.stringify(Json.toJson(poll)))
      )
    val messageTs = slack.postMessage(poll.method, postBody)
    pollRepo.updateMessageTs(poll.id)(messageTs).map { i =>
      poll
    }
  }
}
