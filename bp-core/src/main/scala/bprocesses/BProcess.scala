package main.scala.bprocesses

import main.scala.simple_parts.process._
import main.scala.simple_parts.process.control._
import main.scala.simple_parts.process.data._
import main.scala.bprocesses.links._
import main.scala.utils.links.BPLinkContainer
import main.scala.utils.Space
import main.scala.utils.resources.OwnershipContainer
import main.scala.resources._
import main.scala.simple_parts.process.resource.ResAct

import scala.util.Try

class BProcess(scope: Scope, resources: Option[Array[Resource]] = None, groups: Option[Array[Group]] = None) extends BPLinkContainer[BPLink] 
   with OwnershipContainer
   with BPFlow 
{

/**
 *  Field of process
 */

  var variety: Array[ProcElems] = Array.empty[ProcElems]
  var spaces: Array[Space] = Array.empty[Space]
  links = Array.empty[BPLink]
  var station = new BPStation(this)
  var logger = station.logger
  val marker =  new BPMarker(this)
  val errors = new BottomLine(this)

/**
 *  In-Process collection methods
 */

  def allElements: List[ProcElems] = { 
    flat(
      (variety ++ spaces.collect
        { case space: Space => space.allElements }
      ).toList.asInstanceOf[List[ProcElems]]
    )
  }

  def flat[T](ls: List[T]): List[T]= ls flatten {
      case t: List[T] =>  flat(t)
      case c => List(c)
  }
  def blk = variety.collect { case block: Block ⇒ block }
  def rsl = variety.collect { case brick: Result ⇒ brick }
  def chk = variety.collect { case brick: Brick ⇒ brick }
  def cns = variety.collect { case const: Constant[_] ⇒ const }
  def inputs = variety.collect { case inputs: InputPlaceholder ⇒ inputs }
  def isContain(el: ProcElems) = variety contains el
/**
 * Process search methods
 */
  def fetchObjectById(id: Int) = {
    val frontelem = variety.find(elem => elem.id == id)
    //val space_result = spaces.map(space => space.searchObjById(id))
    //if (!space_result.isEmpty) {
    //  lazy val target_obj = space_result.head // Искомый объект
    //} else 
    //// Поиск по ID для Space
  if (frontelem != None) {
      frontelem
    } else {
      None
    }
  }
  def findObjectByOrder(n: Option[Int]):Option[ProcElems] = {
    variety.find(elem => Some(elem.order) == n)
  }
  def findObjectById(element: Option[Int], space_element:Option[Int]):Option[ProcElems] ={
    allElements.find(elem => Some(elem.id) == element | Some(elem.id) == space_element)
  }

  def findFrontBrickById(id: Option[Int]):Option[Brick] = {
    variety.collect { case brick: Brick => brick }.find(brick => Some(brick.id) == id)
  }
  def findFrontBrick():Array[Brick] = {
    variety.collect { case brick: Brick => brick }
    //new ContainerBrick(4, "container brick", "", Option(CompositeValues()), this, "brick", "containerbrick", 4)
  }
  def findNestedBricks():Array[Brick] = {
    spaces.map(space => space.findBrickInCont).flatten
  }
  def findEverywhereByOrder(n: Int, space: Option[Int] = None):Option[ProcElems] = {
    space match {
      case Some(index) => spaces(index).allElements.find(elem => elem.order == n)
      case _ => allElements.find (elem => elem.order == n)
    }
  }
  def getElemsLength = variety.length - variety.collect { case space: Space => space }.length
  def getSpace(index: Int, space_type:String) = spaces(index-1).space_elems(space_type)
  def getSpaceStep(space_type:String) = space_type match {
    case "container" => station.contStepVal
    case "expands" => station.expStepVal
  }
  def getSpaceByIndex(index: Int) = spaces(index-1)
  def getSpaceQuantity = spaces.length

/**
 * Process CRUD methods
 */

  def updateElem(el: ProcElems, newone: ProcElems, inspace: Boolean = false) = {
    if (!inspace) {
      variety.update(variety.indexOf(el), newone)
    }
    if (inspace) { 
      val space = el.space_id.get
      space.updateElem(el, newone)
    }
    update_link(el, newone)
    println("link updating")
  }

  def addToSpace(elem: ProcElems, space: Space, space_role:String) = {
    space.addToSpace(elem, space_role)
  }
  def orderNum():Int = variety.sortBy(_.order).last.order + 1

/**
 *  Owners
 */
  def owners(b: ResAct)     = ownerships.collect { case link: Ownership ⇒ link.res == b }
  def res_acts(r: Resource) = ownerships.collect { case link: Ownership ⇒ link.resact   == r }


/**
 * Input
 */
  def fill(inputs: SimpleInput) = {
    val z = variety.collect { case placeholder: InputPlaceholder ⇒ placeholder }
    for (x ← z; y ← inputs.in) yield x.push(y)
    // in space
  }
  def pointed_fill(in: PointedInput) = {
    // inspace
    val placeholders: Array[ProcElems] = {
      val arry = in.ids.map(id => fetchObjectById(id).get)
      if (!arry.isEmpty) {
        Array.empty[ProcElems]
      } else {
        arry
      }
    }
    val inputs: List[ProcElems] = in.inputs.toList

    if (!placeholders.isEmpty) {
      for {
             placeholder <- placeholders
             input <- inputs
             if !inputs.isEmpty && !placeholders.isEmpty
            } yield updateElem(placeholder, input, inspace = false) // Not in space
      }
    else { None }
  }

/**
 * Push elements to process
 */

  def pushit(target: Array[ProcElems]) = {
    variety = variety ++ target
  }

  def push(f: ⇒ Array[ProcElems]) = {
    pushit(f)
  }

/**
 * Process elements initialization method call
 */

  def elements_init = {
    println("init elements")
    allElements.map(el => el.init)
  }

/**
 * Temp process elements restoring, after pause
 */

  def restoreProcElems {
   // DEPRECATED
  }


  def inputPmsApply(params: Map[Int, String]) {
    params.foreach {case (key, value) =>
      allElements.find(elem => elem.id == key) match { // TODO: Weak description of Front or Space elems
        case Some(elem) => {
          val tryg = Try(elem.calls(value))
          println(tryg.isSuccess)
        }
        case _ =>
      }

    }
    //for ((id, param) <- params)
  }
  /**
   * Composite values restore
   */
 // TODO: Composite values restore
  def restoreCVOfElems {
    val target = allElements.filter(elem => logger.valChanged(elem))
    println(target)
    // update CV
    target.foreach {
      elem =>
      val changer = logger.valChanger(elem)
      elem.values.get.update(
      n_a_string = changer.a_string,
      n_b_string = changer.a_string,
      n_a_int = changer.a_int,
      n_b_int = changer.b_int,
      n_a_bool = changer.a_bool,
      n_b_bool = changer.b_bool
      )

    }

  }

  /**
   * Copy CompositeValues in heap
   * So all values stay mutable
   * @param values
   * @return copied CV
   */
  def copyCV(values: Option[CompositeValues]):Option[CompositeValues] = {
    values match {
      case None           => None
      case Some(defvalue) => Option(defvalue.copy())
    }
  }

/**
 * Process returning
 */
  //def returning = {
  // find return block
  // execute return block
  // push complete result 
  //}

  /**
   *  Review
   */
   def review(): String = {
    val rev = s"""
      |****************
      |spaces          ${spaces.length}
      |variety         ${variety.length}
      |frontbrick      ${findFrontBrick.length}
      |space_elems     ${spaces.map(sp => sp.container.length)}
      |****************
    """.stripMargin
    spaces.foreach(sp => println(sp.container.length + " " + sp.id))
   rev
 }
}
