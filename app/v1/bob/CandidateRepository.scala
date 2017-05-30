package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.db.DBApi
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.Future

case class Candidate(pollId: Long, serialNo: Int, bobId: Long, name: String = "", style: String = "normal") {
  val insertStmt = s"""($pollId, $serialNo, '$bobId')"""
}

object Candidate {
  implicit val implicitWrites = new Writes[Candidate] {
    def writes(c: Candidate): JsValue = {
      Json.obj(
        "text" -> c.name,
        "name" -> c.pollId,
        "type" -> "button",
        "value" -> c.serialNo,
        "style" -> c.style
      )
    }
  }
}

trait CandidateRepository {
  def select(pollId: Long): Future[Seq[Candidate]]

  def insert(candidates: Seq[Candidate]): Future[Int]
}

@Singleton
class CandidateRepositoryImpl @Inject()(dbapi: DBApi) extends CandidateRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  val simple: RowParser[Candidate] =
    get[Long]("poll_id") ~ get[Int]("serial_no") ~ get[Long]("bob_id") ~ get[String]("bob_name") map {
      case pollId ~ serialNo ~ bobId ~ bobName => Candidate(pollId, serialNo, bobId, bobName)
    }

  def select(pollId: Long): Future[Seq[Candidate]] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL(
          s"""
             |SELECT c.poll_id poll_id, c.serial_no serial_no, b.id bob_id, b.name bob_name
             |FROM candidates c
             |  JOIN bob b ON c.bob_id = b.id
             |WHERE poll_id = {poll_id}
           """.stripMargin
        )
          .on('poll_id -> pollId)
          .as(simple.*)
      }
    }
  }

  def insert(candidates: Seq[Candidate]): Future[Int] = {
    Future.successful {
      db.withConnection { implicit conn =>
        val insert_candidates_stmt: String =
          candidates
            .map(_.insertStmt)
            .mkString(",")

        SQL(
          s"""
             |INSERT INTO candidates (poll_id, serial_no, bob_id)
             |VALUES $insert_candidates_stmt
             |""".stripMargin)
          .executeUpdate

      }
    }
  }
}
