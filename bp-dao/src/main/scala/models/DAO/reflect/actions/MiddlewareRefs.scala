package models.DAO.reflect

import us.ority.min.actions._


import models.DAO.conversion.DatabaseCred
//import models.DAO.driver.MyPostgresDriver.simple._
import models.DAO._
//import models.DAO.driver.MyPostgresDriver.simple._
import models.DAO.conversion.DatabaseFuture._
import com.github.nscala_time.time.Imports._
import models.DAO.conversion.DatabaseCred.dbConfig.driver.api._
import com.github.tototoshi.slick.JdbcJodaSupport._
import main.scala.bprocesses.refs.UnitRefs._

class MiddlewareRefs(tag: Tag) extends Table[MiddlewareRef](tag, "middleware_refs") {
  def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def title       = column[String]("title")

  def ident       = column[String]("ident")
  def ifaceIdent  = column[String]("iface_ident")

  def reaction    = column[Int]("reaction_id")
  def reflection  = column[Int]("reflection_id")

  def created_at  = column[Option[org.joda.time.DateTime]]("created_at")
  def updated_at  = column[Option[org.joda.time.DateTime]]("updated_at")
  def reflectFK   = foreignKey("middleware_ref_reflect_fk", reflection, models.DAO.reflect.RefDAO.refs)(_.id, onDelete = ForeignKeyAction.Cascade)
  def reaction_refFK = foreignKey("middleware_ref_reaction_ref_fk", reaction, models.DAO.reflect.ReactionRefDAO.reaction_refs)(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, title,
           ident,ifaceIdent, reflection, reaction,
           created_at, updated_at) <> (MiddlewareRef.tupled, MiddlewareRef.unapply)
}

object MiddlewareRefsDAOF {
  import akka.actor.ActorSystem
  import akka.stream.ActorFlowMaterializer
  import akka.stream.scaladsl.Source
  import slick.backend.{StaticDatabaseConfig, DatabaseConfig}
  //import slick.driver.JdbcProfile
  //import slick.driver.PostgresDriver.api._
  import slick.jdbc.meta.MTable
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration.Duration
  import scala.concurrent.{ExecutionContext, Awaitable, Await, Future}
  import scala.util.Try

  def await[T](a: Awaitable[T])(implicit ec: ExecutionContext) = Await.result(a, Duration.Inf)
  def awaitAndPrint[T](a: Awaitable[T])(implicit ec: ExecutionContext) = println(await(a))
  val middleware_refs = TableQuery[MiddlewareRefs]


  val create: DBIO[Unit] = middleware_refs.schema.create
  val drop: DBIO[Unit] = middleware_refs.schema.drop


  def ddl_create = db.run(create)
  def ddl_drop = db.run(drop)
  def pull(s: MiddlewareRef):Future[Long] = db.run(middleware_refs returning middleware_refs.map(_.id) += s)


  def getByRef(refId: Int) = db.run(filterByRefQuery(refId).result)

  private def filterByRefQuery(id: Int): Query[MiddlewareRefs, MiddlewareRef, Seq] =
    middleware_refs.filter(_.reflection === id)

  private def filterQuery(id: Long): Query[MiddlewareRefs, MiddlewareRef, Seq] =
    middleware_refs.filter(_.id === id)

}