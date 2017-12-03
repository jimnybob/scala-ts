package com.mpc.scalats.core

import java.io.PrintStream

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.ScalaModel._

import scala.reflect.runtime.universe._

/**
 * Created by Milosz on 11.06.2016.
 */
object TypeScriptGenerator {

  def generateFromClassNames(
    classNames: List[String],
    classLoader: ClassLoader = getClass.getClassLoader,
    out: PrintStream)
    (implicit config: Config) = {

    val mirror = runtimeMirror(classLoader)
    val types = classNames map (mirror.staticClass(_).toType)
    generate(types, out)(config)
  }

  def generate(caseClasses: List[Type], out: PrintStream)(implicit config: Config) = {
    val scalaCaseClasses = ScalaParser.parseCaseClasses(caseClasses)
    validateMaps(scalaCaseClasses) match {
      case Nil => // ok
      case notValid => throw new IllegalArgumentException("Can't generate typescript 'Map' because key won't be serialised to string correctly by Jackson: " + notValid.mkString(","))
    }
    val typeScriptInterfaces = Compiler.compile(scalaCaseClasses)
    TypeScriptEmitter.emit(typeScriptInterfaces, out)
  }

  private def validateMaps(caseClasses: List[Entity]): List[String] = {
    /**
      * Validation based on how Jackson serialises map keys in Spring boot
      * @param keyName must either be 'name' or must have a single property with the same name as the class name
      * @return
      */
    def complexKeyTypeValid(keyName: String): Option[String] = {
      caseClasses.find(_.name == keyName).map(caseClass => caseClass.members match {
        case singleParameter :: Nil =>
          if (singleParameter.name == "name" || singleParameter.name.capitalize == keyName) {
            None
          } else Some(s"Case class ${caseClass.name} has a parameter named '${singleParameter.name}'. It should be either 'name' or '${keyName.head.toLower + keyName.tail}'")
        case head :: tail => Some(s"Case class ${caseClass.name} has more than one parameter")
        case _ => None
      }).getOrElse(throw new IllegalArgumentException(s"Can't find case class for Map with key of type $keyName"))
    }

    def findMapInMember(member: EntityMember): Option[String] = {
      member.typeRef match {
        case MapRef(CaseClassRef(keyName, _, _), _) => complexKeyTypeValid(keyName)
        case MapRef(IntRef | StringRef | LongRef | DoubleRef | FloatRef | BooleanRef, _) => None
        case MapRef(unknown, _) => Some(s"Problem converting unknown map key of type $unknown to string in JSON object")
        case _ => None
      }
    }
    caseClasses.flatMap(entity => entity.members.map(findMapInMember)).flatten
  }

}
