package v1.bob

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.DBApi
import scala.concurrent.Future

final case class BobData(id: Int, name: String)

trait BobRepository {
  def add(name: String): Future[Int]

  def list: Future[Seq[BobData]]

  def getOne(id: Int): Future[Option[BobData]]
}

@Singleton
class BobRepositoryImpl @Inject()(dbapi: DBApi) extends BobRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("default")

  val simple: RowParser[BobData] = {
    get[Int]("id") ~ get[String]("name") map {
      case id ~ name => BobData(id, name)
    }
  }

  override def list: Future[Seq[BobData]] = {
    Future.successful {
      logger.trace(s"list: ")

      db.withConnection { implicit connection =>
        SQL("select id, name from bob ORDER BY id ASC")
          .as(simple.*)
      }
    }
  }


  override def getOne(id: Int): Future[Option[BobData]] = {
    Future.successful {
      logger.trace(s"get: id = $id")

      db.withConnection { implicit c =>
        SQL("select * from bob where id = {id}")
          .on('id -> id)
          .as(simple.singleOpt)
      }
    }
  }

  def add(name: String): Future[Int] = {
    Future.successful {
      logger.trace(s"create: bob = $name")
      db.withConnection { implicit conn =>
        SQL("INSERT INTO bob values ((select next value for bob_seq), {name})")
          .on('name -> name)
          .executeUpdate()
      }
    }
  }
}
