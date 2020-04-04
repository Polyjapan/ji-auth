package ch.japanimpact.auth.api.cas

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class ProxyController @Inject()(cc: ControllerComponents, service: CASService) extends AbstractController(cc) {
  def proxyCallback(pgtId: String, pgtIou: String): Action[AnyContent] = Action {
    service.pgtCallback(pgtIou = pgtIou, pgtId = pgtId)

    Ok
  }
}
