package v1.bob

import javax.inject.{Inject, Provider}

import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PollService @Inject()(routerProvider: Provider[BobRouter],
                            slack: SlackController,
                            bobRepo: BobRepository,
                            pollRepo: PollRepository,
                            candidateRepo: CandidateRepository,
                            voteRepo: VoteRepository)
                           (implicit ec: ExecutionContext) {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def create(channelId: String, bobList: Seq[Bob]): Future[SlackResponse] = {
    createPoll(channelId, bobList)
      .flatMap(identity)
      .flatMap { poll =>
        postPoll(poll).map { _ =>
          SlackResponse("Poll Created")
        }
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

  def vote(v: Vote): Future[SlackResponse] = {
    Try {
      require(Await.result(pollRepo.checkPollOpened(v.pollId), 5 seconds))
    } match {
      case Success(_) => {
        val func: Future[Int] = v.selection match {
          case -1 => pollRepo.closePoll(v.pollId)
          case _ => voteRepo.vote(v)
        }

        func.flatMap { _ =>
          postPoll(getPoll(v.pollId)).map(mkVoteResultString(v)(_))
        }
      }
      case Failure(_) => Future.successful(
        SlackResponse("poll closed!")
      )
    }

  }

  private def mkVoteResultString(v: Vote)(p: BobPoll): SlackResponse = {
    def getBobName(voteSelection: Int) = {
      val bobId: Long = p.candidateBobIdMap.getOrElse(voteSelection, 0)
      bobRepo.asMap.getOrElse(bobId, "Unknown")
    }

    v.selection match {
      case -1 => // close vote
        val result =
          p.resultBySelection
            .mapValues(_.map(_.userMentionStr))
            .map { case (bobId, voters) =>
              s"${getBobName(bobId)}: ${voters.size} - ${voters.mkString(", ")}"
            }
        SlackResponse(
          s"""*Poll Closed!*\n${if (result.isEmpty) "Nothin Selected" else result mkString "\n"}""",
          "in_channel")

      case _ => // vote Action
        val voteList =
          p.resultByUser
            .getOrElse(v.userId, Nil)
            .map(vote => getBobName(vote.selection))
        SlackResponse(s"""You voted to ${if (voteList.isEmpty) "Nowhere" else voteList mkString ", "}.""")
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
