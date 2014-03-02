package com.github.casualjim

import sbt._
import Keys._

object JelasticPlugin extends sbt.Plugin {

  object JelasticKeys {
    val deploy = TaskKey[File]("deploy", "Deploy the war to jelastic")
    val upload = TaskKey[File]("upload", "Upload a file to jelastic")

    // Required keys
    val email = SettingKey[String]("email", "The email address to use as login")
    val password = SettingKey[String]("password", "The password to use for jelastic")
    val apiHoster = SettingKey[String]("api-hoster", "The hoster for the current api")
    val environment = SettingKey[String]("environment", "The environment to deploy into.")

    // Optional keys
    val context = SettingKey[String]("context", "The context to deploy the artifact into")
    val comment = SettingKey[String]("comment", "The comment for the deploy")
    val headers = SettingKey[Map[String, String]]("headers", "HTTP Headers to add")
    val onlyUpload = SettingKey[Boolean]("only-upload", "Just upload the artifact don't deploy it")
  }

  import JelasticKeys._
  import com.earldouglas.xsbtwebplugin.PluginKeys._
  import com.earldouglas.xsbtwebplugin.WarPlugin._

  private def jelasticSettingsIn(c: Configuration) = inConfig(c)( jelasticSettings0 )

  private def jelasticSettings0: Seq[Setting[_]] = Seq(
    comment in deploy := "",
    comment in upload := "",
    context in deploy := "ROOT",
    onlyUpload in deploy := false,
    port in deploy := 443,
    port in upload := 443,
    upload <<= jelasticUploadTask,
    deploy <<= jelasticDeployTask
  )

  val jelasticSettings: Seq[Setting[_]] = jelasticSettingsIn(Compile) ++ Seq(headers := Map.empty)

  import JelasticClient.read
  private def jelasticDeployTask =
    (apiHoster, email in deploy, password in deploy, comment in deploy, onlyUpload in deploy, context in deploy, environment in deploy, headers, port in deploy, packageWar in Compile, streams) map {
      (h, eml, pass, cmt, onlyUpl, ctxt, env, hdrs, p, warFile, s) =>
        val client = new JelasticClient(h, p, hdrs, s.log)
        val auth = client.authenticate(eml, pass)
        if (auth.result == 0) {
          s.log.info("Jelastic session: " + auth.session)
          val uploader = client.upload(auth, warFile)
          if (uploader.result == 0) {
            s.log.info("Upload of %s with size %s succeeded.".format(uploader.file, uploader.size))
            val create = client.createObject(warFile.getName, cmt, uploader, auth)
            if (create.result == 0) {
              s.log.info("File registration for developer " + create.response.obj.developer + " success")
              if (!onlyUpl) {
                val deploy = client.deploy(env, ctxt, create, uploader, auth)
                if (deploy.result == 0) {
                  s.log.info("Deploy success")
                  s.log.info(deploy.response.responses.head.out)
                  val logout = client.logout(auth)
                  if (logout.result == 0) {
                    s.log.info("Logged out of jelastic.")
                  } else {
                    s.log.error("Jelastic logout failed: " + logout.error)
                    throw new RuntimeException(logout.error)
                  }
                } else {
                  s.log.error("Deploy failed: " + deploy.error)
                  throw new RuntimeException(deploy.error)
                }
              }

            }
          } else {
            s.log.error("Jelastic upload of " + warFile.getAbsolutePath + " failed: " + uploader.error)
            throw new RuntimeException(uploader.error)
          }
        } else {
          s.log.error("Jelastic authentication failed: " + auth.error)
          throw new RuntimeException(auth.error)
        }
        warFile
    }

  private def jelasticUploadTask =
    (apiHoster, email in deploy, password in deploy, comment in upload, headers, port in deploy, packageWar in Compile, streams) map {
      (h, eml, pass, cmt, hdrs, p, file, s) =>
        val client = new JelasticClient(h, p, hdrs, s.log)
        val auth = client.authenticate(eml, pass)
        if (auth.result == 0) {
          s.log.info("Jelastic session: " + auth.session)
          val uploader = client.upload(auth, file)
          if (uploader.result == 0) {
            s.log.info("Upload of %s with size %s succeeded.".format(uploader.file, uploader.size))
            val create = client.createObject(file.getName, cmt, uploader, auth)
            if (create.result == 0) {
              s.log.info("File registration for developer " + create.response.obj.developer + " success")
              val logout = client.logout(auth)
              if (logout.result == 0) {
                s.log.info("Logged out of jelastic.")
              } else {
                s.log.error("Jelastic logout failed: " + logout.error)
                throw new RuntimeException(logout.error)
              }
            }
          } else {
            s.log.error("Jelastic upload of " + file.getAbsolutePath + " failed: " + uploader.error)
            throw new RuntimeException(uploader.error)
          }
        } else {
          s.log.error("Jelastic authentication failed: " + auth.error)
          throw new RuntimeException(auth.error)
        }
        file
    }
}
