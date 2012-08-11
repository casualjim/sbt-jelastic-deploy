package com.github.casualjim

import org.specs2._
import execute.Result
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.core.`type`.TypeReference
import java.lang.reflect.{Type, ParameterizedType}
import java.net.URL
import JelasticResponse._

class JsonSpec extends Specification {

  def is =
    "Parsing json should" ^
      entityTest[Authentication]("authentication")(
        _.session must_== "48bxaad71ccc7996325f3803311326b0247d",
        _.error must_== "authentication failed") ^
      entityTest[Create]("createobject")(
        _.response.id must_== 247,
        _.error must_== "invalid parameter [session]") ^
      entityTest[Uploader]("uploader")(
        _.name must_== "jelastic-maven-plugin-1.0-SNAPSHOT.jar",
        _.error must_== "invalid param") ^
      entityTest[Deploy]("deploy")(
        _.response.result must_== 0,
        _.error must_== "application [8129583aae37a4b556d36dbd56abbc68,8129583aae37a4b556d36dbd56abbc68] not exist") ^
      end

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def entityTest[M: Manifest](key: String)(okAssert: M => Result, errorAssert: M => Result) = {
    "when reading %s".format(key) ^
      okTest[M](key)(okAssert) ^
      errorTest[M](key)(errorAssert) ^ bt
  }

  def okTest[M: Manifest](key: String)(assertResult: M => Result) = {
    "read %s ok".format(key) ! {
      val url = getClass.getResource("/" + key + "_ok.json")
      assertResult(deserialize[M](url))
    }
  }

  def errorTest[M: Manifest](key: String)(assertResult: M => Result) = {
    "read %s error".format(key) ! {
      val url = getClass.getResource("/" + key + "_error.json")
      assertResult(deserialize[M](url))
    }
  }

  def deserialize[T: Manifest](value: URL): T =
    mapper.readValue[T](value, new TypeReference[T]() {
      override def getType = new ParameterizedType {
        val getActualTypeArguments = manifest[T].typeArguments.map(_.erasure.asInstanceOf[Type]).toArray
        val getRawType = manifest[T].erasure
        val getOwnerType = null
      }
    })


}
