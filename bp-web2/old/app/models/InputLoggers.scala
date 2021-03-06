package models.DAO

import models.DAO.driver.MyPostgresDriver.api._
import com.github.nscala_time.time.Imports._
import com.github.tototoshi.slick.PostgresJodaSupport._
import slick.model.ForeignKeyAction
import models.DAO.BPDAO._
import models.DAO.resources.BusinessDTO._
import com.github.tminglei.slickpg.composite._
import models.DAO.conversion.{DatabaseCred, Implicits}

import models.DAO._
import models.DAO.resources._

class InputLoggers(tag: Tag) extends Table[InputLogger](tag, "input_loggers") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def uid             = column[Option[String]]("uid")
  def action          = column[String]("action")
  def arguments       = column[List[String]]("arguments")
  def reaction        = column[Int]("reaction_id")
  def input           = column[Int]("input_id", O.AutoInc)
  def date            = column[org.joda.time.DateTime]("date")
  def session         = column[Int]("session_id")

  def maccFK          = foreignKey("in_log_macc_fk", uid, models.AccountsDAO.users)(_.email, onDelete = ForeignKeyAction.Cascade, onUpdate = ForeignKeyAction.Cascade)
  def fReactionFK     = foreignKey("in_log_fReactionFK", reaction, models.DAO.SessionReactionDAO.session_reactions)(_.id, onDelete = ForeignKeyAction.Cascade)
  def sessionFK       = foreignKey("in_log_sessionFK", session, models.DAO.BPSessionDAOF.bpsessions)(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, uid, action, arguments, reaction, input.?, date, session) <> (InputLogger.tupled, InputLogger.unapply)

  //def eb = EmployeesBusinessDAO.employees_businesses.filter(_.employee_id === id).flatMap(_.businessFK)
}

case class InputLogger(var id: Option[Int],
  uid:Option[String]=None,
  action:String,
  arguments:List[String],
  reaction:Int,
  input:Option[Int],
  date: org.joda.time.DateTime,
  session: Int)

object InputLoggerDAOF {
  import akka.actor.ActorSystem


  import slick.backend.{StaticDatabaseConfig, DatabaseConfig}
  //import slick.driver.JdbcProfile
  import slick.driver.PostgresDriver.api._
  import slick.jdbc.meta.MTable
  import scala.concurrent.ExecutionContext.Implicits.global
  import com.github.tototoshi.slick.PostgresJodaSupport._
  import scala.concurrent.duration.Duration
  import scala.concurrent.{ExecutionContext, Awaitable, Await, Future}
  import scala.util.Try
  import models.DAO.conversion.DatabaseFuture._
  def await[T](a: Awaitable[T])(implicit ec: ExecutionContext) = Await.result(a, Duration.Inf)
  def awaitAndPrint[T](a: Awaitable[T])(implicit ec: ExecutionContext) = println(await(a))
  val input_loggers = InputLoggerDAO.input_loggers

  //private def filterQueryByProcess(process: Int): Query[ProcessHistoriesF, ProcessHistoryDTO, Seq] =
  //  bpsessions.filter(_.process === process)
  private def filterQuery(id: Int): Query[InputLoggers, InputLogger, Seq] =
    input_loggers.filter(_.id === id)
  private def filterByBPQuery(id: Int): Query[InputLoggers, InputLogger, Seq] =
    (for {
      ((inlogger, session), bprocesses) <- input_loggers joinLeft
      models.DAO.BPSessionDAOF.bpsessions on (_.session === _.id) joinLeft BPDAO.bprocesses on (_._2.map(_.process) === _.id)
    } yield (inlogger))
  private def filterBySessionQuery(id: Int): Query[InputLoggers, InputLogger, Seq] =
    input_loggers.filter(_.session === id)

  private def filterBySessionsQuery(ids: List[Int]): Query[InputLoggers, InputLogger, Seq] =
    input_loggers.filter(_.session inSetBind ids)


def launchesPeoplesFetcher(sessionStatuses: List[SessionStatus]):Future[List[SessionStatus]] = {
    //val launchedBy = ???
    val act_permsF = ActPermissionDAOF.getAllByProcessesId(sessionStatuses.map(status => status.process.id.get).toList)
    act_permsF.flatMap { all_act_perms =>
      val groupsF = AccountGroupDAOF.getAllByGroupIDS(all_act_perms.map(_.group.getOrElse(-1)).toList)
      val loggersF = getBySessions(sessionStatuses.map(status => status.session.id.get).toList)
      loggersF.flatMap { loggers =>
        groupsF.map { groups =>
          sessionStatuses.map { sessionStatus => // Iterate over each session
               val act_perms = all_act_perms.filter(perm => perm.process == sessionStatus.process.id.get)
                                               .filter(perm => perm.role == "interact" || perm.role == "all")
               val participators = act_perms.map(_.uid).flatten ++ groups.filter(group => act_perms.map(_.group)
                                                                         .flatten.contains(group.id.get))
                                                                         .flatMap(_.account_id)
                                                                         .distinct // remove dublicates
              loggers.find(logger => logger.session == sessionStatus.session.id.get).headOption match {
                case Some(firstInputLog) => {
                  sessionStatus.copy(peoples = Some(SessionPeoples(
                              launched_by = firstInputLog.uid.getOrElse("not@found.com"),
                              participators = participators.toSet.toList)))
                }
                case _ => sessionStatus.copy(peoples = Some(SessionPeoples(launched_by = "not@found.com",
                  participators = participators.toSet.toList)))
              }
          }
        }
      }
    }
}
/*****
  Because in first bp-dao project no account models presented we make this polyfil
  It's copy session container and add people and participator list of String
******/
def fetchPeopleBySessions(sessionContainers: Seq[SessionContainer]):Future[Seq[SessionContainer]] = {
    val sessionStatuses = sessionContainers.map(cn => cn.sessions).flatten.toList
    val containersF = launchesPeoplesFetcher(sessionStatuses)
    containersF.map { container =>
      sessionContainers.map { sessionContainer =>
        sessionContainer.copy(sessions = container
                                          .filter( containered => containered.process.id == sessionContainer.process.id ))
      }
    }
}




  def get(id: Int):Future[Option[InputLogger]] = db.run(filterQuery(id).result.headOption)
  def getByBP(BPid:Int)                        = db.run(filterByBPQuery(BPid).result)
  def getBySession(session_id: Int)            = db.run(filterBySessionQuery(session_id).result)
  def getBySessions(session_ids: List[Int])    = db.run(filterBySessionsQuery(session_ids).result)

}

object InputLoggerDAO {
  import akka.actor.ActorSystem
  import slick.backend.{StaticDatabaseConfig, DatabaseConfig}
  //import slick.driver.JdbcProfile
  import slick.driver.PostgresDriver.api._
  import slick.jdbc.meta.MTable
  import scala.concurrent.ExecutionContext.Implicits.global
  import com.github.tototoshi.slick.PostgresJodaSupport._
  import scala.concurrent.duration.Duration
  import scala.concurrent.{ExecutionContext, Awaitable, Await, Future}
  import scala.util.Try
  import models.DAO.conversion.DatabaseFuture._

  //import dbConfig.driver.api._ //
  def await[T](a: Awaitable[T])(implicit ec: ExecutionContext) = Await.result(a, Duration.Inf)
  def awaitAndPrint[T](a: Awaitable[T])(implicit ec: ExecutionContext) = println(await(a))


 val input_loggers = TableQuery[InputLoggers]

 def pull_for_input(s: List[InputLogger]) =   {

    val first_one_id = s.headOption match {
      case Some(first_one) => Some(pull_object(first_one))
      case _ => None
    }

    val first_one = first_one_id match {
      case Some(first_one_id) => get(first_one_id)
      case _ => None
    }

    val input_id = first_one match {
      case Some(first_one) => first_one.input
      case _ => None
    }
    input_id match {
      case Some(input_id) => s.tail.foreach { tailed_inlog =>
        pull_object(tailed_inlog.copy(input = Some(input_id)))
      }
      case _ =>
    }
 }

 def pull_object(s: InputLogger) =   {
      await(db.run(  input_loggers returning input_loggers.map(_.id) += s ))
  }
  def get(k: Int) =   {
      val q3 = for { s ← input_loggers if s.id === k } yield s
      await(db.run(q3.result.headOption))
  }


  def getByBP(BPid:Int) =   {
    val q3 = (for {
      ((inlogger,
        session),
        bprocesses) <-
        input_loggers joinLeft models.DAO.BPSessionDAOF.bpsessions on (_.session === _.id) joinLeft BPDAO.bprocesses on (_._2.map(_.process) === _.id)
    } yield (inlogger))
    await(db.run(q3.result)).toList
  }
  def getBySession(session_id: Int) =   {
    val q3 = for { s <- input_loggers if s.session === session_id } yield s
    await(db.run(q3.result)).toList
  }
  def getBySessions(ids: List[Int]) =   {
    val q3 = for { s <- input_loggers if s.session inSetBind ids } yield s
    await(db.run(q3.result)).toList
  }

  def update(id: Int, obj: InputLogger) =   {
    val toUpdate: InputLogger = obj.copy(Option(id))
    await(db.run(  input_loggers.filter(_.id === id).update(toUpdate) ))
  }

  def delete(id: Int) =   {
    await(db.run(  input_loggers.filter(_.id === id).delete ))
  }

  def getAll =   {
      val q3 = for { s ← input_loggers } yield s
      await(db.run(q3.result)).toList.sortBy(_.id)
  }


  def launchPeopleFetcher(sessionStatus: SessionStatus):SessionStatus = {
    //val launchedBy = ???
     val act_perms = ActPermissionDAO.getByProcessId(sessionStatus.process.id.get).filter(perm => perm.role == "interact" || perm.role == "all")
     val participators = act_perms.map(_.uid).flatten ++ AccountGroupDAO.getAllByGroupIDS(act_perms.map(_.group).flatten)
     .flatMap(_.account_id)
     .distinct // remove dublicates
    getBySession(sessionStatus.session.id.get).headOption match {
      case Some(firstInputLog) => {
        sessionStatus.copy(peoples = Some(SessionPeoples(
                    launched_by = firstInputLog.uid.getOrElse("not@found.com"),
                    participators = participators.toSet.toList)))
      }
      case _ => sessionStatus.copy(peoples = Some(SessionPeoples(launched_by = "not@found.com",
        participators = participators.toSet.toList)))
    }
  }
  /* Because in first bp-dao project no account models presented we make this polyfil
     It's copy session container and add people and participator list of String
  */
  def fetchPeople(sessionContainer: SessionContainer):SessionContainer = {
    sessionContainer.copy(sessions = sessionContainer.sessions.map(st => launchPeopleFetcher(st)))
  }


  val create: DBIO[Unit] = input_loggers.schema.create
  val drop: DBIO[Unit] = input_loggers.schema.drop

  def ddl_create = db.run(create)
  def ddl_drop = db.run(drop)

}
