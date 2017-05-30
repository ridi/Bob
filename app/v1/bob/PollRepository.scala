package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.db.DBApi
import scala.concurrent.Future

case class Poll(id: Long, messageTs: Option[String], isOpen: Boolean)

case class PollResult(pollId: Long, userId: String, candidateSerialNo: Int, candidateName: String) {
  val userMentionStr = s"<@$userId>"
}

trait PollRepository {
  def select(pollId: Long): Future[Poll]

  def createNewPoll(): Future[Long]

  def closePoll(pollId: Long): Future[Int]

  def checkPollOpened(pollId: Long): Future[Boolean]

  def updateMessageTs(id: Long)(messageTs: String): Future[Int]

  def getPollResult(pollId: Long): Future[Seq[PollResult]]
}

@Singleton
class PollRepositoryImpl @Inject()(dbapi: DBApi) extends PollRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  def select(pollId: Long): Future[Poll] = {
    Future.successful {
      db.withConnection { implicit conn =>
        val simple: RowParser[Poll] = {
          get[Long]("id") ~ get[Option[String]]("message_ts") ~ get[Boolean]("is_open") map {
            case id ~ msgTs ~ isOpen => Poll(id, msgTs, isOpen)
          }
        }

        SQL("SELECT id, message_ts, is_open FROM poll WHERE id = {id}")
          .on('id -> pollId)
          .as(simple.single)
      }
    }
  }

  def createNewPoll(): Future[Long] = {
    Future.successful {
      logger.trace(s"create new poll")
      db.withConnection { implicit conn =>
        SQL("""INSERT INTO poll (is_open) values (true)""")
          .executeInsert(scalar[Long].single)
      }
    }
  }

  def closePoll(pollId: Long): Future[Int] = {
    Future.successful {
      logger.trace(s"close poll (id: $pollId)")
      db.withConnection { implicit conn =>
        SQL("""UPDATE poll SET is_open = false WHERE id = {id}""")
          .on('id -> pollId)
          .executeUpdate
      }
    }
  }

  def checkPollOpened(pollId: Long): Future[Boolean] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL("""SELECT is_open FROM poll WHERE id = {id}""")
          .on('id -> pollId)
          .as(scalar[Boolean].single)
      }
    }
  }

  def updateMessageTs(id: Long)(messageTs: String): Future[Int] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL("UPDATE poll SET message_ts = {message_ts} WHERE id = {id}")
          .on(
            'id -> id,
            'message_ts -> messageTs
          ).executeUpdate
      }
    }
  }

  def getPollResult(pollId: Long): Future[Seq[PollResult]] = {
    Future.successful(
      db.withConnection { implicit conn =>
        val simple: RowParser[PollResult] = {
          get[String]("user_id") ~ get[Int]("serial_no") ~ get[String]("name") map {
            case userId ~ serialNo ~ name => PollResult(pollId, userId, serialNo, name)
          }
        }

        SQL(
          """
            |SELECT vh.user_id user_id, vh.candidate_serial_no serial_no, bob.name name
            |FROM vote_history vh
            |  JOIN candidates c ON vh.poll_id = c.poll_id AND vh.candidate_serial_no = c.serial_no
            |  JOIN bob ON c.bob_id = bob.id
            |WHERE vh.poll_id = {poll_id}
            |GROUP BY user_id, serial_no, name
            |HAVING count(*) % 2 = 1
          """.stripMargin)
          .on('poll_id -> pollId)
          .as(simple.*)
      }
    )
  }
}
