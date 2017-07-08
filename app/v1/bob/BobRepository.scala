package v1.bob

import javax.inject.{Inject, Singleton}

import anorm._
import anorm.SqlParser._
import play.api.cache._
import play.api.db.DBApi

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

final case class Bob(id: Long, name: String)

trait BobRepository {
  val simple: RowParser[Bob] = {
    get[Long]("id") ~ get[String]("name") map {
      case id ~ name => Bob(id, name)
    }
  }

  def add(name: String): Future[Int]

  def getList: Future[Seq[Bob]]

  val list: Seq[Bob]

  val asMap: Map[Long, String]
}

@Singleton
class BobRepositoryImpl @Inject()(cache: CacheApi, dbapi: DBApi) extends BobRepository {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  private val db = dbapi.database("bob")

  val list: Seq[Bob] = cache.getOrElse("bob"){
    val bob = Await.result(getList, 10 seconds)
    bob
  }

  override def getList: Future[Seq[Bob]] = {
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

  val asMap: Map[Long, String] = list.map(l => l.id -> l.name).toMap
}
