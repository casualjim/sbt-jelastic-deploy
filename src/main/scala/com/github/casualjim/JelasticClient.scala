package com.github.casualjim

import dispatch._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.core.`type`.TypeReference
import java.lang.reflect.{Type, ParameterizedType}
import java.io.{File, InputStream}
import com.ning.http.client._
import com.ning.http.multipart.{StringPart, FilePart}
import com.ning.http.client.AsyncHandler.STATE
import sbt.Logger

object JelasticClient {

  val Scheme = "https"
  val ApiVersion = "1.0"
  val AuthenticationPath = "%s/users/authentication/rest/signin".format(ApiVersion)
  val UploaderPath = "%s/storage/uploader/rest/upload".format(ApiVersion)
  val CreatePath = "deploy/createObject"
  val DeployPath = "deploy/DeployArchive"
  val LogoutPath = "users/authentication/rest/signout"

  val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)

  private[this] def deserialize[T: Manifest](src: InputStream): T = 
    mapper.readValue[T](src, new TypeReference[T]() {
      override def getType = new ParameterizedType {
        val getActualTypeArguments = manifest[T].typeArguments.map(_.erasure.asInstanceOf[Type]).toArray
        val getRawType = manifest[T].erasure
        val getOwnerType = null
      }
    })

  object read {


    private def response[M: Manifest] = as.Response(r => deserialize[M](r.getResponseBodyAsStream))

    val Authentication = response[JelasticResponse.Authentication]
    val Create = response[JelasticResponse.Create]
    val Uploader = response[JelasticResponse.Uploader]
    val Deploy = response[JelasticResponse.Deploy]
    val Logout = response[JelasticResponse.Logout]

  }

}

class JelasticClient(apiHoster: String = "j.layershift.co.uk", port: Int = 443, headers: Map[String, String] = Map.empty, logger: Logger) {

  import JelasticClient._

  private[this] var cookies: Seq[Cookie] = Nil

  import scala.collection.JavaConverters._

  private implicit def headerVerb(req: RequestBuilder) = new {
    def <:<(headers: Map[String, String]) = {
      headers foreach {
        case (name, value) => req.addHeader(name, value)
      }
      req
    }

    def <<<(params: Map[String, Any]) = {
      req.setMethod("POST")
      req.setHeader("Content-Type", "multipart/form-data")
      val parts = params collect {
        case (key, s: String) => new StringPart(key, s)
        case (key, f: File) => new FilePart(key, f)
      }
      parts foreach req.addBodyPart
      req
    }

    def withCookies = {
      cookies foreach req.addCookie
      req
    }
  }

  private val defaultPorts = Seq(80, 443)
  private def reqBase: RequestBuilder = {
    val h = apiHoster
    val u = if (!defaultPorts.contains(port)) :/(h, port) else :/(h)
    u.secure <:< headers
  }


  private val keepCookies = (resp: Response) => {
    val nw = resp.getCookies.asScala.toSeq
    cookies = cookies.filterNot(c => nw.exists(_ == c.getName)) ++ nw
    cookies foreach {
      cookie =>
        logger.debug("Cookie: %s with value: %s" format(cookie.getName, cookie.getValue))
    }
    resp
  }

  private val clearCookies = (resp: Response) => {
    cookies = Nil
    resp
  }


  def authenticate(login: String, password: String): JelasticResponse.Authentication = {
    val req = reqBase / AuthenticationPath
    logger.debug("Making authentication request to: " + req.build.toString)
    logger.debug("login: " + login)
    logger.debug("password: " + password)

    Http(req <<? Map("login" -> login, "password" -> password) OK (clearCookies andThen keepCookies andThen read.Authentication))()
  }

  def upload(auth: JelasticResponse.Authentication, file: File): JelasticResponse.Uploader = {
    val req = reqBase / UploaderPath
    logger.debug("Making upload request to: " + req.build.toString)
    logger.debug("file: " + file.getAbsolutePath)
    val pcfg = new PerRequestConfig()
    pcfg.setRequestTimeoutInMs(300000)
    req.setPerRequestConfig(pcfg)
    val uploader = new AsyncCompletionHandler[JelasticResponse.Uploader] {
      def onCompleted(response: Response): JelasticResponse.Uploader = (keepCookies andThen read.Uploader)(response)

      private var numSt = 0L

      override def onContentWriteProgress(amount: Long, current: Long, total: Long): STATE = {
        val nw = (amount / total) * 100
        if (nw != numSt) {
          logger.info("[" + nw + "%]")
          numSt = nw
        }
        super.onContentWriteProgress(amount, current, total)
      }
    }

    Http(req.withCookies <<< Map("fid" -> "123456", "session" -> auth.session, "file" -> file) > uploader)()
  }

  def createObject(name: String, comment: String, uploader: JelasticResponse.Uploader, auth: JelasticResponse.Authentication): JelasticResponse.Create = {
    val dataFmt = """{"name":"%s", "archive":"%s", "link":0, "size":%s, "comment":"%s"}"""
    val data = dataFmt.format(name, uploader.file, uploader.size, comment)
    val params = Map(
      "charset" -> "UTF-8",
      "session" -> auth.session,
      "type" -> "JDeploy",
      "data" -> data)

    logger.debug("Making create request to: " + (reqBase / CreatePath).build.toString)
    logger.debug("params: %s" format params)
    Http(reqBase.withCookies / CreatePath << params OK (keepCookies andThen read.Create))()
  }

  def deploy(environment: String, context: String, create: JelasticResponse.Create, uploader: JelasticResponse.Uploader, auth: JelasticResponse.Authentication): JelasticResponse.Deploy = {
    val params = Map(
      "charset" -> "UTF-8",
      "session" -> auth.session,
      "archiveUri" -> uploader.file,
      "archiveName" -> uploader.name,
      "newContext" -> context,
      "domain" -> environment
    )
    logger.debug("Making deploy request to: " + (reqBase / DeployPath).build.toString)
    logger.debug("params: %s" format params)
    Http(reqBase.withCookies / DeployPath << params OK (keepCookies andThen read.Deploy))()
  }

  def logout(auth: JelasticResponse.Authentication): JelasticResponse.Logout = {
    val params = Map("charset" -> "UTF-8", "session" -> auth.session)
    logger.debug("Making logout request to: " + (reqBase / LogoutPath).build.toString)
    logger.debug("params: %s" format params)
    Http(reqBase.withCookies / LogoutPath <<? params OK (clearCookies andThen read.Logout))()
  }
}
