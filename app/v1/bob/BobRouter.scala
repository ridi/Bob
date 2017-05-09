package v1.bob

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class BobRouter @Inject()(controller: BobController)
  extends SimpleRouter {
  val prefix = "/slack/bob"

  def link(id: Int): String = {
    import com.netaporter.uri.dsl._
    val url = prefix / id.toString
    url.toString()
  }

  override def routes: Routes = {
    case GET(p"/") => controller.index

    case GET(p"/$id") => controller.show(id.toInt)

    case POST(p"/") => controller.process
  }
}
