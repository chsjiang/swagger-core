package com.wordnik.swagger.core

import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import com.wordnik.swagger.annotations.{AllowableValueIgnored, AllowableValuesByReflection}
import java.lang.reflect.Modifier

trait BaseApiParser {
  private val logger = LoggerFactory.getLogger(classOf[BaseApiParser])

  val POSITIVE_INFINITY_STRING = "Infinity"
  val NEGATIVE_INFINITY_STRING = "-Infinity"

  protected def readString(s: String, existingValue: String = null, ignoreValue: String = null): String = {
    if (existingValue != null && existingValue.trim.length > 0) existingValue
    else if (s == null) null
    else if (s.trim.length == 0) null
    else if (ignoreValue != null && s.equals(ignoreValue)) null
    else s.trim
  }

  protected def toObjectList(csvString: String, paramType: String = null) = {
    if (csvString == null || csvString.length == 0) new ListBuffer[String].toList
    else {
      val params = csvString.split(",").toList
      paramType match {
        case null => params
        case "string" => params
      }
    }
  }

  protected def convertToAllowableValues(csvString: String, paramType: String = null): Option[DocumentationAllowableValues] = {
    if (csvString.toLowerCase.startsWith("range[")) {
      val ranges = csvString.substring(6, csvString.length() - 1).split(",")
      return Some(buildAllowableRangeValues(ranges, csvString))
    } else if (csvString.toLowerCase.startsWith("rangeexclusive[")) {
      val ranges = csvString.substring(15, csvString.length() - 1).split(",")
      return Some(buildAllowableRangeValues(ranges, csvString))
    } else if (csvString.toLowerCase.startsWith("class[") ){
      return convertToAllowableValuesByReflection(csvString.substring(6, csvString.length -1))
    } else {
      if (csvString == null || csvString.length == 0) {
        None
      } else {
        val params = csvString.split(",").toList
        paramType match {
          case null => Some(new DocumentationAllowableListValues(params.asJava))
          case "string" => Some(new DocumentationAllowableListValues(params.asJava))
        }
      }
    }
  }

  protected def convertToAllowableValuesByReflection(className : String) : Option[DocumentationAllowableValues] = {
    if (className == null || className.length == 0) {
      None
    } else {
      val clazz = Class.forName(className)
      val annot = clazz.getAnnotation(classOf[AllowableValuesByReflection]);
      if (annot == null) {
        None
      }
      else {
      val values =
      for {
        f <- clazz.getDeclaredFields
           if (f.getAnnotation(classOf[AllowableValueIgnored]) == null && !Modifier.isStatic(f.getModifiers))
      } yield f.getName
      Some(new DocumentationAllowableListValues(values.toList asJava))
      }
    }

  }

  private def buildAllowableRangeValues(ranges: Array[String], inputStr: String): DocumentationAllowableRangeValues = {
    var min: java.lang.Float = 0
    var max: java.lang.Float = 0
    if (ranges.size < 2) {
      throw new RuntimeException("Allowable values format " + inputStr + "is incorrect")
    }
    if (ranges(0).equalsIgnoreCase(POSITIVE_INFINITY_STRING)) {
      min = Float.PositiveInfinity
    } else if (ranges(0).equalsIgnoreCase(NEGATIVE_INFINITY_STRING)) {
      min = Float.NegativeInfinity
    } else {
      min = ranges(0).toFloat
    }
    if (ranges(1).equalsIgnoreCase(POSITIVE_INFINITY_STRING)) {
      max = Float.PositiveInfinity
    } else if (ranges(1).equalsIgnoreCase(NEGATIVE_INFINITY_STRING)) {
      max = Float.NegativeInfinity
    } else {
      max = ranges(1).toFloat
    }
    val allowableValues = new DocumentationAllowableRangeValues(min, max)
    allowableValues
  }
}