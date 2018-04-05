package com.mpc.scalats.core

/**
 * Created by Milosz on 09.06.2016.
 */

import com.mpc.scalats.configuration.Config

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

object ScalaParser {

  import ScalaModel._

  private val unwantedBaseClasses = Seq("Any", "AnyVal", "Immutable", "Iterable", "Poly", "Poly1", "PolyApply", "Object", "Product", "Equals", "Serializable", "Ordered", "Comparable", "Function1", "PartialFunction")

  private val unwantedTraitDefs = Seq("toString", "productArity", "productElement","canEqual")

  def parseCaseClasses(classTypes: List[Type])(implicit config: Config): List[Entity] = {
    val involvedTypes = classTypes flatMap getInvolvedTypes(Set.empty)
    val typesToParse = (involvedTypes filter isEntityType).distinct
    (typesToParse map parseType).distinct
  }

  private def packageName(sym: Symbol) = {
    def enclosingPackage(sym: Symbol): Symbol = {
      if (sym == NoSymbol) NoSymbol
      else if (sym.isPackage) sym
      else enclosingPackage(sym.owner)
    }

    val pkg = enclosingPackage(sym)
    if (pkg == currentMirror.EmptyPackageClass) ""
    else pkg.fullName
  }

  private def isInAllowedPackages(clazz: Symbol)(implicit config: Config): Boolean = {
    if(config.onlyPackages.nonEmpty) {
      config.onlyPackages.find(packageName(clazz).startsWith(_)).nonEmpty
    } else true
  }

  private def parseType(aType: Type)(implicit config: Config) = {
    val relevantMemberSymbols = aType.members.collect {
      case m: MethodSymbol if m.isAccessor => m
    }
    val traitDefs = aType.members.collect {
      case m: MethodSymbol if m.isAbstract && !unwantedTraitDefs.contains(m.name.toString) => m
    }
    val typeParams = aType.typeConstructor.dealias.etaExpand match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decodedName.toString)
      case _ => List.empty[String]
    }
    val members = (relevantMemberSymbols ++ traitDefs) map { member =>
      val memberName = member.name.toString
      EntityMember(memberName, getTypeRef(member.returnType, typeParams.toSet))
    }
    val thisClassName = aType.typeSymbol.name.toString
    Entity(
      thisClassName,
      members.toList,
      typeParams,
      aType.baseClasses.filter { baseClass =>
        !(thisClassName + unwantedBaseClasses).contains(baseClass.name.toString) && isInAllowedPackages(baseClass)
      }.map(_.name.toString),
      aType.typeSymbol.asClass.isTrait
    )
  }

  private def getInvolvedTypes(alreadyExamined: Set[Type])(scalaType: Type)(implicit config: Config): List[Type] = {
    if (!alreadyExamined.contains(scalaType) && !scalaType.typeSymbol.isParameter) {
      val relevantMemberSymbols = scalaType.members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      }
      val memberTypes = relevantMemberSymbols.map(_.typeSignature match {
        case NullaryMethodType(resultType) => resultType
        case t => t
      }).flatMap(getInvolvedTypes(alreadyExamined + scalaType))
      val typeArgs = scalaType match {
        case t: scala.reflect.runtime.universe.TypeRef => t.args.flatMap(getInvolvedTypes(alreadyExamined + scalaType))
        case _ => List.empty
      }

      var subClasses = Set.empty[Type]
      if(scalaType.typeSymbol.asClass.isTrait && scalaType.typeSymbol.asClass.isSealed) {
        subClasses = scalaType.typeSymbol.asClass.knownDirectSubclasses.map(_.info)
      }

      val superClasses = scalaType.typeSymbol.asClass.baseClasses.collect { case baseClass if isInAllowedPackages(baseClass) && !unwantedBaseClasses.contains(baseClass.name.toString) =>
        baseClass.info
      }.toSet

      (scalaType.typeConstructor :: typeArgs ::: memberTypes.toList ::: subClasses.toList ::: superClasses.toList).filter(!_.typeSymbol.isParameter).distinct
    } else {
      List.empty
    }
  }

  private def getTypeRef(scalaType: Type, typeParams: Set[String]): TypeRef = {
    val typeName = scalaType.typeSymbol.name.toString
    typeName match {
      case "Int" | "Byte" =>
        IntRef
      case "Long" =>
        LongRef
      case "Double" =>
        DoubleRef
      case "Float" =>
        FloatRef
      case "Boolean" =>
        BooleanRef
      case "String" =>
        StringRef
      case "List" | "Seq" | "Set" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        SeqRef(getTypeRef(innerType, typeParams))
      case "Map" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        val otherType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.last
        MapRef(getTypeRef(innerType, typeParams), getTypeRef(otherType, typeParams))
      case "Option" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        OptionRef(getTypeRef(innerType, typeParams))
      case "LocalDate" =>
        DateRef
      case "Instant" | "Timestamp" =>
        DateTimeRef
      case typeParam if typeParams.contains(typeParam) =>
        TypeParamRef(typeParam)
      case _ if isEntityType(scalaType) =>
        val caseClassName = scalaType.typeSymbol.name.toString
        val typeArgs = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args
        val typeArgRefs = typeArgs.map(getTypeRef(_, typeParams))

        CaseClassRef(caseClassName, typeArgRefs)
      case _ =>
        UnknownTypeRef(typeName)
    }
  }

  private def isNotScalaCollectionMember(classSymbol: ClassSymbol) =
    !classSymbol.fullName.startsWith("scala.collection.")

  private def isEntityType(scalaType: Type) = {
    val typeSymbol = scalaType.typeSymbol
    if (typeSymbol.isClass) {
      val classSymbol = typeSymbol.asClass
      isNotScalaCollectionMember(classSymbol) && (classSymbol.isCaseClass || classSymbol.isTrait)
    }
    else false
  }

}