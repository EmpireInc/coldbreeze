package models.DAO.reflect


import models.DAO.driver.MyPostgresDriver.simple._
import scala.slick.model.ForeignKeyAction
import com.github.nscala_time.time.Imports._
import models.DAO.BPDAO._
import models.DAO.resources.BusinessDTO._
import com.github.tminglei.slickpg.composite._
import models.DAO.conversion.{DatabaseCred, Implicits}
import main.scala.bprocesses.refs.UnitRefs._
import main.scala.simple_parts.process.Units._  
  
  

class SpaceElementReflections(tag: Tag) extends Table[UnitSpaceElementRef](tag, "space_element_reflections") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def reflection = column[Int]("reflection_id")
  def title = column[String]("title")
  def desc  = column[String]("desc")
  def b_type = column[String]("b_type")
  def type_title = column[String]("type_title")

  def ref_space_own = column[Option[Int]]("ref_space_id")
  def ref_space_owned = column[Int]("ref_space_owned_id")

  def space_role = column[Option[String]]("space_role")
  def order = column[Int]("order")
    
  def created_at = column[Option[org.joda.time.DateTime]]("created_at")
  def updated_at = column[Option[org.joda.time.DateTime]]("updated_at")  
    
  def reflectFK = foreignKey("reflect_fk", reflection, models.DAO.reflect.RefDAO.refs)(_.id, onDelete = ForeignKeyAction.Cascade)


    
  def * = (id.?, 
reflection, 
title, 
desc, 
b_type, 
type_title, 
ref_space_own, 
ref_space_owned, 
space_role, 
order,
created_at, updated_at) <> (UnitSpaceElementRef.tupled, UnitSpaceElementRef.unapply)

}

import main.scala.simple_parts.process._
import main.scala.bprocesses._





/**
 * Actions
 */
object SpaceElementReflectionDAO {
  import DatabaseCred.database
  import models.DAO.BPDAO.bprocesses


  val space_element_reflections = TableQuery[SpaceElementReflections]

  def lastOrderOfSpace(reflection: Int, space_id: Int):Int = {
    database withSession { implicit session =>
       val q3 = for { el ← space_element_reflections if el.reflection === reflection && el.ref_space_owned === space_id } yield el
       val xs = q3.list.map(_.order)
      if (xs.isEmpty) 1
      else xs.max + 1
    }
  }

  def pull_object(s: UnitSpaceElementRef) = database withSession {
    implicit session ⇒
      space_element_reflections returning space_element_reflections.map(_.id) += s
  }
  def findById(id: Int) = {
     database withSession { implicit session =>
       val q3 = for { s ← space_element_reflections if s.id === id } yield s
       q3.list.headOption                   
    } 
  }
  def findBySpace(space_id: Int) = {
     database withSession { implicit session =>
       val q3 = for { s ← space_element_reflections if s.ref_space_owned === space_id } yield s
       q3.list                   
    } 
  }
  def findByRef(id: Int) = {
     database withSession { implicit session =>
       val q3 = for { s ← space_element_reflections if s.reflection === id } yield s
       q3.list                   
    } 
  }
  def findByElemRefs(ids: List[Int]) = {
     database withSession { implicit session =>
       val q3 = for { s ← space_element_reflections if s.ref_space_owned inSetBind ids } yield s
       q3.list                   
    } 
  }
  def retrive(k: Int, business: Int, process: Int,space_own:Option[Int],
                        ref_space_owned: Int):List[UnitSpaceElement] = database withSession {
    implicit session =>
      findByRef(k).map(e => e.reflect(business, process, space_own, ref_space_owned))

  }
  def update(id: Int, entity: UnitSpaceElementRef):Boolean = {
    database withSession { implicit session =>
      findById(id) match {
      case Some(e) => {
        space_element_reflections.filter(_.id === id).update(entity)
        true
      }
      case None => false
      }
    }
  }
  def getAll = database withSession {
    implicit session ⇒ 
      val q3 = for { s ← space_element_reflections } yield s
      q3.list.sortBy(_.id)
  }
  def delete(id: Int) = { 
    database withSession { implicit session ⇒

    val elem = findById(id)
    space_element_reflections.filter(_.id === id).delete
    }
  }

def moveUp(reflection: Int, element_id: Int, space_id: Int) = {
    database withSession { implicit session =>
      val minimum = findByRef(reflection).sortBy(_.order)
      findById(element_id) match {
        case Some(e) => { 
          if (e.order > 1 && e.order != minimum.head.order) {
            space_element_reflections
              .filter(_.id === element_id).update(e.copy(order = e.order - 1))
            val ch = findById(minimum.find(_.order == (e.order - 1)).get.id.get).get
            space_element_reflections
              .filter(_.id === minimum.find(_.order == (e.order - 1)).get.id.get).update(ch.copy(order = ch.order + 1))
          }
          true 
        }
        case None => false
      }
    }
  }
  def moveDown(reflection: Int, element_id: Int, space_id: Int) = {
    database withSession { implicit session =>
      val maximum = findBySpace(space_id).sortBy(_.order)
      findById(element_id) match {
        case Some(e) => { 
          if (e.order < maximum.last.order && e.order != maximum.last.order) {
            space_element_reflections
              .filter(_.id === element_id).update(e.copy(order = e.order + 1))
            val ch = findById(maximum.find(_.order == (e.order + 1)).get.id.get).get
            space_element_reflections
              .filter(_.id === maximum.find(_.order == (e.order + 1)).get.id.get).update(ch.copy(order = ch.order - 1))
          }
          true 
        }
        case None => false
      }
    }
  }

  def ddl_create = {
    database withSession {
      implicit session =>
      space_element_reflections.ddl.create
    }
  }
  def ddl_drop = {
    database withSession {
      implicit session =>
        space_element_reflections.ddl.drop
    }
  }
  
}