package ru.circumflex.core

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.net.URLDecoder

class CircumflexContext(val request: HttpServletRequest,
                        val response: HttpServletResponse,
                        val filter: AbstractCircumflexFilter)
    extends HashModel {

  /**
   * A helper for getting and setting response headers in a DSL-like way.
   */
  object header extends HashModel {

    def apply(name: String): Option[String] = request.getHeader(name)

    def update(name: String, value: String) { response.setHeader(name, value) }

    def update(name: String, value: Long) { response.setDateHeader(name, value) }

    def update(name: String, value: java.util.Date) { update(name, value.getTime) }

  }

  /**
   * A helper for getting and setting session-scope attributes.
   */
  object session extends HashModel {

    def apply(name: String): Option[Any] = request.getSession.getAttribute(name)

    def update(name: String, value: Any) = request.getSession.setAttribute(name, value)

  }

  /**
   * A helper for setting flashes. Flashes provide a way to pass temporary objects between requests.
   */
  object flash extends HashModel {

    private val _key = "cx.flash"

    def apply(key: String): Option[Any] = {
      val flashMap = session.getOrElse(_key, MutableMap[String, Any]())
      flashMap.get(key) map { value => {
        session(_key) = flashMap - key
        value
      }}
    }

    def update(key: String, value: Any) {
      val flashMap = session.getOrElse(_key, MutableMap[String, Any]())
      session(_key) = flashMap + (key -> value)
    }

  }

  // ### Content type

  protected var _contentType: String = null

  def contentType: Option[String] = _contentType

  def contentType_=(value: String) { _contentType = value }

  // ### Status code

  var statusCode: Int = 200

  // ### Method

  def method: String = getOrElse('_method, request.getMethod)

  // ### Request parameters

  private val _params = MutableMap[String, Any](
    "header" -> header,
    "session" -> session,
    "flash" -> flash
  )
  
  def getString(key: String): Option[String] = get(key) map { _.toString }

  def apply(key: String): Option[Any] = _params.get(key) match {
    case Some(value) if (value != null) => value
    case _ => request.getParameter(key)
  }

  def update(key: String, value: Any) { _params += key -> value }
  def +=(pair: (String, Any)) { _params += pair }

  // ### Request URI

  /*
   * To manage "RouterResponse", for example on
   *   any("/sub*") = new SubRouter
   * the main router deletes the "/sub" prefix from the uri
   * before to forward the request to the sub router.
   * At sub router level redirect and rewrites are relatives.
   */

  var uri = ""       // relative uri
  var uriPrefix = "" // prefix of relative uri

  def getAbsoluteUri(relUri: String) = uriPrefix + relUri

  private[core] def updateUri =
    getMatch('uri) foreach { uriMatch =>
      if (uriMatch.value == uri) {
        uriPrefix += uriMatch.prefix
        uri = uriMatch.suffix
        
        // "/prefix/", "path" => "/prefix", "/path"
        if (uriPrefix.endsWith("/")) {
          uriPrefix = uriPrefix.take(uriPrefix.length - 1)
          uri = "/" + uri
        }
      }
    }

  // ### Request matching

  private[core] var _matches = Map[String, Match]()

  def getMatch(key: String): Option[Match] = _matches.get(key)

}

object CircumflexContext {
  private val threadLocalContext = new ThreadLocal[CircumflexContext]

  def context = threadLocalContext.get

  def isOk = context != null

  def init(req: HttpServletRequest,
           res: HttpServletResponse,
           filter: AbstractCircumflexFilter) {
    if (!isOk) {
      threadLocalContext.set(new CircumflexContext(req, res, filter))
      Circumflex.messages(req.getLocale) match {
        case Some(msg) => context('msg) = msg
        case None =>
          cxLog.debug("Could not instantiate context messages: 'cx.messages' not configured.")
      }
    }

    context.uri = URLDecoder.decode(req.getRequestURI, "UTF-8")
  }

  def destroy() = threadLocalContext.set(null)
}