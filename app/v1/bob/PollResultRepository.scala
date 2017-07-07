package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.db.DBApi
import scala.concurrent.Future

case class PollResult(pollId: Long, bobId: Long)

trait PollResultRepository {
  def select(pollId: Long): Future[List[PollResult]]

  def insert(result: PollResult): Future[PollResult]

  def getRecentlySelected(channelId: String): Future[List[Long]]

  val simple: RowParser[PollResult] = {
    get[Long]("poll_id") ~ get[Long]("bob_id") map {
      case pollId ~ bobId => PollResult(pollId, bobId)
    }
  }
}

@Singleton
class PollResultRepositoryImpl @Inject()(dbapi: DBApi) extends PollResultRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  def select(pollId: Long): Future[List[PollResult]] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL("SELECT poll_id, bob_id FROM poll_result WHERE poll_id = {poll_id}")
          .on('poll_id -> pollId)
          .as(simple.*)
      }
    }
  }

  def insert(result: PollResult): Future[PollResult] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL("INSERT INTO poll_result VALUES ({poll_id}, {bob_id})")
          .on('poll_id -> result.pollId, 'bob_id -> result.bobId)
          .executeInsert(simple.single)
      }
    }
  }

  def getRecentlySelected(channelId: String): Future[List[Long]] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL(
          """
            |SELECT r.bob_id
            |FROM poll_result r
            |  JOIN poll p
            |WHERE p.channel_id = {channelId}
            |ORDER BY p.message_ts DESC
            |LIMIT 5
          """.stripMargin)
        .on('channelId -> channelId)
        .as(scalar[Long].*)
      }
    }
  }
}
