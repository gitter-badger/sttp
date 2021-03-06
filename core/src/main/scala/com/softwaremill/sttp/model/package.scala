package com.softwaremill.sttp

import java.io.InputStream
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.file.Path

import scala.language.higherKinds
import scala.collection.immutable.Seq

package object model {
  case class Method(m: String) extends AnyVal
  object Method {
    val GET = Method("GET")
    val HEAD = Method("HEAD")
    val POST = Method("POST")
    val PUT = Method("PUT")
    val DELETE = Method("DELETE")
    val OPTIONS = Method("OPTIONS")
    val PATCH = Method("PATCH")
    val CONNECT = Method("CONNECT")
    val TRACE = Method("TRACE")
  }

  /**
    * Provide an implicit value of this type to serialize arbitrary classes into a request body.
    * Handlers might also provide special logic for serializer instances which they define (e.g. to handle streaming).
    */
  type BodySerializer[T] = T => BasicRequestBody

  sealed trait RequestBody
  case object NoBody extends RequestBody
  // TODO: extract StreamBody, with request parametrized to match the stream type?
  case class SerializableBody[T](f: BodySerializer[T], t: T)
      extends RequestBody

  sealed trait BasicRequestBody extends RequestBody
  case class StringBody(s: String, encoding: String) extends BasicRequestBody
  case class ByteArrayBody(b: Array[Byte]) extends BasicRequestBody
  case class ByteBufferBody(b: ByteBuffer) extends BasicRequestBody
  case class InputStreamBody(b: InputStream) extends BasicRequestBody
  case class PathBody(f: Path) extends BasicRequestBody

  /**
    * @tparam T Target type as which the response will be read.
    * @tparam S If `T` is a stream, the type of the stream. Otherwise, `Nothing`.
    */
  sealed trait ResponseAs[T, +S]

  object IgnoreResponse extends ResponseAs[Unit, Nothing]
  case class ResponseAsString(encoding: String)
      extends ResponseAs[String, Nothing]
  object ResponseAsByteArray extends ResponseAs[Array[Byte], Nothing]
  case class ResponseAsStream[T, S]()(implicit val responseIsStream: S =:= T)
      extends ResponseAs[T, S]
  case class ResponseAsParams(encoding: String)
      extends ResponseAs[Seq[(String, String)], Nothing] {

    def parse(s: String): Seq[(String, String)] = {
      s.split("&")
        .toList
        .flatMap(kv =>
          kv.split("=", 2) match {
            case Array(k, v) =>
              Some(
                (URLDecoder.decode(k, encoding),
                 URLDecoder.decode(v, encoding)))
            case _ => None
        })
    }
  }
}
