package v1.bob

import javax.inject.Inject

import play.api.http.HttpVerbs
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class BobRequest[A](request: Request[A], val messages: Messages) extends WrappedRequest(request)

class BobAction @Inject()(messagesApi: MessagesApi)
                         (implicit ec: ExecutionContext)
  extends ActionBuilder[BobRequest] with HttpVerbs {
  type SlackRequestBlock[A] = BobRequest[A] => Future[Result]

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  override def invokeBlock[A](request: Request[A], block: SlackRequestBlock[A]): Future[Result] = {
    if (logger.isTraceEnabled()) {
      logger.trace(s"invokeBlock: request = $request")
    }

    val messages = messagesApi.preferred(request)
    val future = block(new BobRequest(request, messages))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case other =>
          result
      }
    }
  }
}
