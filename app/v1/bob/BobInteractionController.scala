package v1.bob

import javax.inject.Inject

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class BobInteractionFormInput(payload: String)

class BobInterActionController @Inject()(action: BobAction,
                                         handler: BobResourceHandler)
                                        (implicit ec: ExecutionContext)
  extends Controller {

  private val form: Form[BobInteractionFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "payload" -> nonEmptyText
      )(BobInteractionFormInput.apply)(BobInteractionFormInput.unapply)
    )
  }

  def process: Action[AnyContent] = {
    action.async { implicit request =>
      processInteraction()
    }
  }

  private def processInteraction[A]()(implicit request: BobRequest[A]): Future[Result] = {
    def failure(badForm: Form[BobInteractionFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: BobInteractionFormInput): Future[Result] = {
      handler.processReaction(input.payload).map { msg =>
        Created(Json.toJson(msg))
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
}
