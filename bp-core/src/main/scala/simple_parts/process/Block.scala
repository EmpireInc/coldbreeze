/**
 *
 *
 */
package main.scala.simple_parts.process

import scala.collection.mutable._
import main.scala.simple_parts.process._
import main.scala.bprocesses.InvokeTracer
import main.scala.utils.ElementTracer
import main.scala.utils.Space
import main.scala.bprocesses._

/**
 * @author Sobolev
 */

class Block(val id: Int,
  title: String, 
  desc: String, 
  values: CompositeValues,
  bprocess: BProcess,
  b_type: String, 
  b_title: String,
  var order: Int,
  val space_parent: Option[Space],
  val space_role: Option[String]
  ) extends ProcElems {



  override def toString = s"Block: $title"

  override def invoke {
    println("invoked block")
  }

  def expand(obj: ProcElems) = {
    if (space_role == "expands") {
      // Change element
      // Change link of that element
    }
  }
}
