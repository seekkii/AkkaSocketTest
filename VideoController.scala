package controllers

import com.google.inject.{Inject, Singleton}
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.JavaFlowSupport.Source
import org.apache.pekko.stream.scaladsl.{Flow, Source}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request, WebSocket}

import scala.concurrent.Future

@Singleton
class VideoController @Inject()
(implicit val materializer: Materializer,
 implicit val actorRef: ActorRef,
 cc: ControllerComponents
)extends AbstractController(cc){

  val flow = Flow[Any].map(_=>"Hello")
  def socket:WebSocket = WebSocket.acceptOrResult[Any,String]{
    request =>
      Future.successful(
        Right(flow)
      )
  }

  def stream() : Action[AnyContent] = Action.async{ implicit request : Request[AnyContent] =>
    val source = Source.single(request.body)

  }

}
