package models.DAO


import models.DAO.driver.MyPostgresDriver1.simple._
import scala.slick.model.ForeignKeyAction
import models.DAO.BPDAO._
import models.DAO.resources.BusinessDTO._
import com.github.tminglei.slickpg.composite._
import models.DAO.conversion.{DatabaseCred, Implicits}


class BPStations(tag: Tag) extends Table[BPStationDTO](tag, "bpstations") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def process = column[Int]("process_id")

  def state = column[Boolean]("state")
  def step = column[Int]("step")
  def space = column[Int]("space")
  def container_step = column[List[Int]]("container_step")
  def expand_step = column[List[Int]]("expand_step")
  def started = column[Boolean]("started")
  def finished = column[Boolean]("finished")
  def inspace = column[Boolean]("inspace")
  def incontainer = column[Boolean]("incontainer")
  def inexpands = column[Boolean]("inexpands")
  def paused = column[Boolean]("paused")


  def * = (id.?,
    process,
    state,
    step,
    space,
    container_step,
    expand_step,
    started,
    finished,
    inspace,
    incontainer,
    inexpands,
    paused) <> (BPStationDTO.tupled, BPStationDTO.unapply)

}

/*
  Case class
 */
case class BPStationDTO(
id: Option[Int],
process: Int,
state:Boolean,
step:Int,
space:Int,
container_step: List[Int],
expand_step: List[Int],
started: Boolean,
finished: Boolean,
inspace: Boolean,
incontainer: Boolean,
inexpands: Boolean,
paused: Boolean)

/*
  DataConversion
 */
object BPStationDCO {

}


object BPStationDAO {
  import models.DAO.BPDAO.bprocesses
  import main.scala.bprocesses.BPStation

  import DatabaseCred.database

  val bpstations = TableQuery[BPStations]

  def from_origin_station(station: BPStation, bp_dto: BProcessDTO):BPStationDTO = {
    BPStationDTO(
        None,
        bp_dto.id.get,
        station.state,
        station.step,
        station.space,
        station.container_step.toList,
        station.expand_step.toList,
        station.started,
        station.finished,
        station.inspace,
        station.incontainer,
        station.inexpands,
        station.paused)
  }
  //def to_origin_station(station: BPStationDTO1):BPStation = {

  //}

  def pull_object(s: BPStationDTO) = database withSession {
    implicit session ⇒
      bpstations returning bpstations.map(_.id) += s //BPStationDTO.unapply(s).get
  }

  def findByBPId(id: Int) = {
    database withSession { implicit session =>
     val q3 = for { st ← bpstations if st.process === id } yield st// <> (BPStationDTO.tupled, BPStationDTO.unapply _)

      q3.list 
    }
  }
  def getAll = database withSession {
    implicit session ⇒ // TODO: s.service === 1 CHANGE DAT
      val q3 = for { s ← bpstations } yield s
      q3.list.sortBy(_.id)
    //suppliers foreach {
    //  case (id, title, address, city, state, zip) ⇒
    //    Supplier(id, title, address, city, state, zip)
    //}
  }
def update(id: Int, entity: BPStationDTO):Boolean = {
    database withSession { implicit session =>
      findById(id) match {
      case Some(e) => {
        bpstations.where(_.id === id).update(entity)
        true
      }
      case None => false
      }
    }
  }
  def areActiveForBP(id: Int) = {
     database withSession { implicit session =>
        val q3 = for { st ← bpstations 
          if st.process === id 
          if st.finished === false
          } yield st// <> (BPStationDTO.tupled, BPStationDTO.unapply _)

        q3.list
     }
  }
  def findById(id: Int):Option[BPStationDTO] = {
    database withSession {
    implicit session =>
      val q3 = for { s <- bpstations if s.id === id } yield s// <> (BPStationDTO.tupled, BPStationDTO.unapply _)

      q3.list.headOption //.map(Supplier.tupled(_))
    }
  }
  /*def ddl_create = {
    database withSession {
      implicit session =>
        bpstations.ddl.create
    }
  }*/

}