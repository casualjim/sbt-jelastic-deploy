package com.github.casualjim

import com.fasterxml.jackson.annotation.{JsonProperty, JsonIgnoreProperties}

object JelasticResponse {
  case class Cpu(usage: String, time: String)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class Debug(time: String, cpu: Cpu)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class Authentication(
                             uid: String,
                             result: Int,
                             session: String,
                             email: String,
                             name: String,
                             debug: Debug,
                             error: String)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class CreateObject(id: Int, developer: String, uploadDate: String)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class CreateResponse(id: Int, result: Int, @JsonProperty("object") obj: CreateObject, error: Option[String] = None)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class Create(
                     result: Int,
                     @JsonProperty("object") response: CreateResponse,
                     error: String,
                     debug: Debug)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class DeployResult(result: Int, nodeId: Int, out: String)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class DeployResponse(result: Int, out: String, error: String, responses: List[DeployResult] = Nil)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class Deploy(result: Int, response: DeployResponse, error: String, debug: Debug)

  case class Logout(result: Int, error: String, debug: Debug)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class UploaderRequest(fid: String, session: String, appid: String, file: String, charset: String)

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class Uploader(
                       result: Int,
                       file: String,
                       request: UploaderRequest,
                       name: String,
                       size: Int,
                       error: String)
}