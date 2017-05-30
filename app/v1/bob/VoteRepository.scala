package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.db.DBApi
import scala.concurrent.Future

case class Vote(pollId: Long, userId: String, selection: Int) {
  val userMentionStr = s"<@$userId>"
}

trait VoteRepository {
  def select(pollId: Long): Future[Seq[Vote]]

  def vote(v: Vote): Future[Int]
}

@Singleton
class VoteRepositoryImpl @Inject()(dbapi: DBApi) extends VoteRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  val simple: RowParser[Vote] =
    get[Long]("poll_id") ~ get[String]("user_id") ~ get[Int]("candidate_serial_no") map {
      case pollId ~ userId ~ candidateSerialNo => Vote(pollId, userId, candidateSerialNo)
    }

  def select(pollId: Long): Future[Seq[Vote]] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL(
          """SELECT poll_id, user_id, candidate_serial_no
            |FROM vote_history
            |WHERE poll_id = {poll_id}
          """.stripMargin)
          .on('poll_id -> pollId)
          .as(simple.*)
      }
    }
  }

  def vote(v: Vote): Future[Int] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL("INSERT INTO vote_history VALUES ({poll_id}, {user_id}, {candidate_serial_no})")
          .on(
            'poll_id -> v.pollId,
            'user_id -> v.userId,
            'candidate_serial_no -> v.selection
          )
          .executeUpdate
      }
    }
  }
}
