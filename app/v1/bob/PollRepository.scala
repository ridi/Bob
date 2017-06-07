package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.db.DBApi
import scala.concurrent.Future

case class Poll(id: Long, channelId: String, messageTs: Option[String], isOpen: Boolean)

trait PollRepository {
  def select(pollId: Long): Future[Poll]

  def createNewPoll(channelId: String): Future[Long]

  def closePoll(pollId: Long): Future[Int]

  def checkPollOpened(pollId: Long): Future[Boolean]

  def updateMessageTs(id: Long)(messageTs: String): Future[Int]
}

@Singleton
class PollRepositoryImpl @Inject()(dbapi: DBApi) extends PollRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  def select(pollId: Long): Future[Poll] = {
    Future.successful {
      db.withConnection { implicit conn =>
        val simple: RowParser[Poll] = {
          get[Long]("id") ~ get[String]("channel_id") ~ get[Option[String]]("message_ts") ~ get[Boolean]("is_open") map {
            case id ~ channelId ~ msgTs ~ isOpen => Poll(id, channelId, msgTs, isOpen)
          }
        }

        SQL("SELECT id, channel_id, message_ts, is_open FROM poll WHERE id = {id}")
          .on('id -> pollId)
          .as(simple.single)
      }
    }
  }

  def createNewPoll(channelId: String): Future[Long] = {
    Future.successful {
      logger.trace(s"create new poll")
      db.withConnection { implicit conn =>
        SQL(s"""INSERT INTO poll (channel_id, is_open) values ('$channelId', true)""")
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
}
