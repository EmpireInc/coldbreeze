package models.DAO

import models.DAO.driver.MyPostgresDriver1.simple._
import com.github.nscala_time.time.Imports._
import models.DAO.conversion.DatabaseCred

class InputLoggers(tag: Tag) extends Table[InputLogger](tag, "input_loggers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def uid = column[Option[String]]("uid")
  def action = column[String]("action")
  def arguments = column[List[String]]("arguments")
  def front_elem_id = column[Option[Int]]("front_elem_id")
  def space_elem_id = column[Option[Int]]("space_elem_id")
  def date      = column[org.joda.time.DateTime]("date")


  def fElemFK = foreignKey("fElemFK", front_elem_id, models.DAO.ProcElemDAO.proc_elements)(_.id, onDelete = ForeignKeyAction.Cascade)
  def spElemFK = foreignKey("spElemFK", space_elem_id, models.DAO.SpaceElemDAO.space_elements)(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, uid, action, arguments, front_elem_id, space_elem_id, date) <> (InputLogger.tupled, InputLogger.unapply)

  //def eb = EmployeesBusinessDAO.employees_businesses.filter(_.employee_id === id).flatMap(_.businessFK)
}

case class InputLogger(var id: Option[Int], uid:Option[String]=None, action:String, arguments:List[String], front_elem_id:Option[Int], space_elem_id:Option[Int], date: org.joda.time.DateTime)


object InputLoggerDAO {
  import scala.util.Try
  import DatabaseCred.database

 val input_loggers = TableQuery[InputLoggers]

 def pull_object(s: InputLogger) = database withSession {
    implicit session ⇒
      
      input_loggers returning input_loggers.map(_.id) += s
  }
  def get(k: Int) = database withSession {
    implicit session ⇒
      val q3 = for { s ← input_loggers if s.id === k } yield s
      q3.list.headOption 
  }

  

  def update(id: Int, obj: InputLogger) = database withSession { implicit session ⇒
    val toUpdate: InputLogger = obj.copy(Option(id))
    input_loggers.filter(_.id === id).update(toUpdate)
  }

  def delete(id: Int) = database withSession { implicit session ⇒

    input_loggers.filter(_.id === id).delete
  }
  def count: Int = database withSession { implicit session ⇒
    Query(input_loggers.length).first
  }  
  def getAll = database withSession {
    implicit session ⇒
      val q3 = for { s ← input_loggers } yield s 
      q3.list.sortBy(_.id)
  
  }

  def ddl_create = {
    database withSession {
      implicit session =>
      input_loggers.ddl.create
    }
  }

}



