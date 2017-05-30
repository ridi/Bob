package v1.bob

import javax.inject.Inject

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class BobFormInput(text: String)

class BobController @Inject()(action: BobAction,
                              handler: BobResourceHandler)
                             (implicit ec: ExecutionContext)
  extends Controller {

  private val form: Form[BobFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "text" -> nonEmptyText
      )(BobFormInput.apply)(BobFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = {
    action.async { implicit request =>
      handler.getList.map { bobList =>
        Ok(Json.toJson(bobList))
      }
    }
  }

  def process: Action[AnyContent] = {
    action.async { implicit request =>
      processJsonPost()
    }
  }

  private def processJsonPost[A]()(implicit request: BobRequest[A]): Future[Result] = {
    def failure(badForm: Form[BobFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: BobFormInput): Future[Result] = {
      handler.process(input).map { msg =>
        Created(Json.toJson(msg))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
}
