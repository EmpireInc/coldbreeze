package models.DAO.resources

import models.DAO.driver.MyPostgresDriver.simple._
import models.DAO.conversion.DatabaseCred

class Clients(tag: Tag) extends Table[(Option[Int], String)](tag, "clients") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")



  def * = (id.?, title) //<> (Supplier.tupled, Supplier.unapply)

  def cb = ClientBusinessDAO.clients_businesses.filter(_.client_id === id).flatMap(_.businessFK)


}

case class ClientDTO(var id: Option[Int], title: String)

object ClientDAO {
  import scala.util.Try
  import DatabaseCred.database

  val clients = TableQuery[Clients]




  def pull_object(s: ClientDTO) = database withSession {
    implicit session ⇒
      val tuple = ClientDTO.unapply(s).get
      clients returning clients.map(_.id) += (value = (None, s.title))//(BusinessDTO.unapply(s).get._2, BusinessDTO.unapply(s).get._3)
  }

  def pull(id: Option[Int] = None, title: String) = Try(database withSession {
    implicit session ⇒

      clients += (id, title)
  }).isSuccess

  def get(k: Int) = database withSession {
    implicit session ⇒
      val q3 = for { s ← clients if s.id === k } yield s <> (ClientDTO.tupled, ClientDTO.unapply _)
      println(q3.selectStatement)
      println(q3.list)
      q3.list.headOption //.map(Supplier.tupled(_))
  }

  def getBusiness(k: Int) = database withSession {
    implicit session ⇒
      val q1 = (for { 
      s ← clients if s.id === k
      j <- s.cb
      } yield (s.id, j.title))
      println("Manual join")
      println(q1.selectStatement)

      println(q1.run.toSet)
      q1.run.toSet
      //q1.list.head //.map(Supplier.tupled(_))
  }
  /**
   * Update a client
   * @param id
   * @param client
   */
  def update(id: Int, client: ClientDTO) = database withSession { implicit session ⇒
    val bpToUpdate: ClientDTO = client.copy(Option(id))
    clients.where(_.id === id).update(ClientDTO.unapply(bpToUpdate).get)
  }
  /**
   * Delete a client
   * @param id
   */
  def delete(id: Int) = database withSession { implicit session ⇒

    clients.where(_.id === id).delete
  }
  /**
   * Count all clients
   */
  def count: Int = database withSession { implicit session ⇒
    Query(clients.length).first
  }



  def getAll = database withSession {
    implicit session ⇒
      val q3 = for { s ← clients } yield s <> (ClientDTO.tupled, ClientDTO.unapply _)
      q3.list.sortBy(_.id)
    //suppliers foreach {
    //  case (id, title, address, city, state, zip) ⇒
    //    Supplier(id, title, address, city, state, zip)
    //}
  }

  def ddl_create = {
    database withSession {
      implicit session =>
      clients.ddl.create
    }
  }

}


