package models.DAO

import main.scala.bprocesses.BProcess
import main.scala.simple_parts.process.{Block, ProcElems}
import models.DAO.driver.MyPostgresDriver.simple._
import scala.slick.model.ForeignKeyAction
import models.DAO.conversion.{DatabaseCred, Implicits}

import main.scala.simple_parts.process.data.{Confirm, Note, Constant}
import main.scala.simple_parts.process.Block
import main.scala.simple_parts.process.ContainerBrick
import main.scala.utils.Space

class SpaceElements(tag: Tag) extends Table[SpaceElementDTO](tag, "space_elements") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def title = column[String]("title")
  def desc  = column[String]("desc")
  def business = column[Int]("business_id")
  def bprocess = column[Int]("bprocess_id")
  def b_type = column[String]("b_type")
  def type_title = column[String]("type_title")

  def space_own = column[Option[Int]]("own_space_id")
  def space_owned = column[Int]("owned_space_id")
  def space_role = column[Option[String]]("space_role")

  def order = column[Int]("order")
  def comps = column[Option[List[CompositeValues]]]("comps", O.DBType("compositevalues[]"))
  def * = (id.?, title, desc,  business,
           bprocess,   b_type, type_title,
           space_own,  space_owned,
           space_role, order, comps) <> (SpaceElementDTO.tupled, SpaceElementDTO.unapply)

  def businessFK = foreignKey("business_fk", business, models.DAO.resources.BusinessDAO.businesses)(_.id, onDelete = ForeignKeyAction.Cascade)
  def bpFK = foreignKey("bprocess_fk", bprocess, models.DAO.BPDAO.bprocesses)(_.id, onDelete = ForeignKeyAction.Cascade)
  def spaceFK = foreignKey("bpspace_fk", space_owned, models.DAO.BPSpaceDAO.bpspaces)(_.id, onDelete = ForeignKeyAction.Cascade)
  // TODO: Space FK

}

/*
  Case class
 */

case class SpaceElementDTO(id: Option[Int],
                        title:String,
                        desc:String,
                        business:Int,
                        bprocess:Int,
                        b_type:String,
                        type_title:String,
                        space_own:Option[Int],
                        space_owned: Int,
                        space_role:Option[String],
                        order:Int,
                        comps: Option[List[CompositeValues]]) {
  def cast(process: BProcess, space_dto: BPSpaceDTO):Option[ProcElems] = {
    println("block castiong")
    this match {
      case x if (x.b_type == "block" && x.type_title == "test block") => {
        Option(
          new Block(id.get,title,desc,Implicits.fetch_cv(comps),process,b_type,type_title,order)
        )
      }
      case constant if (constant.b_type == "block" && constant.type_title == "constant") => {
        Option(
          new Constant[Boolean](id.get, true, process, order, space_id = process.spaces.find(space => space.index == space_dto.index))
        )
      }
      case note if (note.b_type == "block" && note.type_title == "note") => Option(new Note( id.get,
        title,
        desc,
        Implicits.fetch_cv(comps),
        process,
        b_type,
        type_title,
        order,
        None))
      case confirm if (confirm.b_type == "block" && confirm.type_title == "confirm") => Option(new Confirm( id.get,
        title,
        desc,
        Implicits.fetch_cv(comps),
        process,
        b_type,
        type_title,
        order,
        None))
      case _ => None
    }

  }

  def castToSpace(process: BProcess, space: Space):Option[ProcElems] = {
    println("block castiong")
    this match {
      case x if (x.b_type == "brick" | x.type_title == "container_brick") => {
        println("space_parent " + process.spaces.find(_.id == space_own))
        println("space_parent REFACTOR!!!!!!!" + space_own)
        // TODO REFACTOR space_parent in brick
        Option(
          new ContainerBrick(id.get, title, desc,Implicits.fetch_cv(comps), process, b_type, type_title, order, 
            None, space_role.get, space_own)
          //new Block(id.get,title,desc,Implicits.fetch_cv(comps),process,b_type,type_title,order, space_parent = Some(space), space_role)
        )
      }
      case constant if (constant.b_type == "block" && constant.type_title == "constant") => {
        Option(
          new Constant[Boolean](id.get, true, process, order, space_id = Some(space))
        )
      }
      case note if (note.b_type == "block" && note.type_title == "note") => Option(new Note( id.get,
        title,
        desc,
        Implicits.fetch_cv(comps),
        process,
        b_type,
        type_title,
        order,
        None))
      case confirm if (confirm.b_type == "block" && confirm.type_title == "confirm") => Option(new Confirm( id.get,
        title,
        desc,
        Implicits.fetch_cv(comps),
        process,
        b_type,
        type_title,
        order,
        None))
      case _ => None
    }

  }
}
/**
 * Actions
 */
object SpaceElemDAO {
  import DatabaseCred.database
  import models.DAO.BPDAO.bprocesses


  val space_elements = TableQuery[SpaceElements]
  /**
   * Find a specific entity by id.
   */
  def findByBPId(id: Int) = {
    database withSession { implicit session =>
      val q3 = for { el ← space_elements if el.bprocess === id } yield el
      q3.list
    }
  }
  def findBySpace(space_id: Int) = {
    database withSession { implicit session =>
     val q3 = for { el <- space_elements if el.space_owned === space_id } yield el
     q3.list
    }
  }
  def findById(id: Int):Option[SpaceElementDTO] = {
    database withSession { implicit session =>
      val q3 = for { el ← space_elements if el.id === id } yield el
      q3.list.headOption
    }
  }
  def findByBPanOrder(id: Int, order: Int) = {
    database withSession { implicit session =>
      val q3 = for { el ← space_elements if el.bprocess === id; if el.order === order } yield el
      q3.list.headOption
    }
  }
  def pull_object(s: SpaceElementDTO) = database withSession {
    implicit session ⇒
      space_elements returning space_elements.map(_.id) += s
  }
  def ddl_create = {
    database withSession {
      implicit session =>
      space_elements.ddl.create
    }
  }
  def update(id: Int, entity: SpaceElementDTO):Boolean = {
    database withSession { implicit session =>
      findById(id) match {
        case Some(e) => { space_elements.filter(_.id === id).update(entity); true }
        case None => false
      }
    }
  }
  def delete(id: Int) = {
   database withSession { implicit session ⇒

    val elem = findById(id)
    val res = space_elements.filter(_.id === id).delete
    elem match {
       case Some(el) => renewOrder(el.bprocess, el.space_owned, el.order)
       case _ =>
    }
    res 
  }
  
  }

def moveUp(bprocess: Int, element_id: Int, space_id: Int) = {
    database withSession { implicit session =>
      val minimum = findByBPId(bprocess).sortBy(_.order)
      findById(element_id) match {
        case Some(e) => { 
          if (e.order > 1 && e.order != minimum.head.order) {
            space_elements.filter(_.id === element_id).update(e.copy(order = e.order - 1))
            val ch = findById(minimum.find(_.order == (e.order - 1)).get.id.get).get
            space_elements.filter(_.id === minimum.find(_.order == (e.order - 1)).get.id.get).update(ch.copy(order = ch.order + 1))
          }
          true 
        }
        case None => false
      }
    }
  }
  def moveDown(bprocess: Int, element_id: Int, space_id: Int) = {
    database withSession { implicit session =>
      val maximum = findBySpace(space_id).sortBy(_.order)
      findById(element_id) match {
        case Some(e) => { 
          if (e.order < maximum.last.order && e.order != maximum.last.order) {
            space_elements.filter(_.id === element_id).update(e.copy(order = e.order + 1))
            val ch = findById(maximum.find(_.order == (e.order + 1)).get.id.get).get
            space_elements.filter(_.id === maximum.find(_.order == (e.order + 1)).get.id.get).update(ch.copy(order = ch.order - 1))
          }
          true 
        }
        case None => false
      }
    }
  }

/*
(1,Some(16))
(3,Some(17))
(4,Some(18))
(6,Some(19))
.renewOrder(bprocess, 5)
(1,Some(16))
(3,Some(17))
(4,Some(18))
(5,Some(19))
*/
  def renewOrder(bprocess: Int, space_id: Int, order_num: Int) = {
    database withSession { implicit session ⇒
      val q3 = for { el ← space_elements if el.bprocess === bprocess && el.space_owned === space_id && el.order > order_num } yield el
      val ordered = q3.list.zipWithIndex.map(el => el._1.copy(order = (el._2 + 1) + (order_num - 1)))
      ordered.foreach { el => 
         update(el.id.get, el)
      }
    }

    
/*

    proc_elements.filter(_.bprocess === bprocess && _.order > order_num)
     .map(x => x.order)
     .update(_ + 1)

    */
  }

}