package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.db.DBApi
import scala.concurrent.Future

final case class Bob(id: Long, name: String)

trait BobRepository {
  val simple: RowParser[Bob] = {
    get[Long]("id") ~ get[String]("name") map {
      case id ~ name => Bob(id, name)
    }
  }

  def add(name: String): Future[Int]

  def list: Future[Seq[Bob]]
}

@Singleton
class BobRepositoryImpl @Inject()(dbapi: DBApi) extends BobRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  override def list: Future[Seq[Bob]] = {
    Future.successful {
      db.withConnection { implicit connection =>
        SQL("SELECT id, name FROM bob ORDER BY id ASC")
          .as(simple.*)
      }
    }
  }

  def add(name: String): Future[Int] = {
    Future.successful {
      db.withConnection { implicit conn =>
        SQL("INSERT INTO bob (name) values ({name})")
          .on('name -> name)
          .executeUpdate()
      }
    }
  }
}
