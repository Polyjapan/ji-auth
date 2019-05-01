package controllers.management

import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{email, mapping}
import play.api.libs.mailer.MailerClient
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class GroupMembersCRUDController @Inject()(cc: MessagesControllerComponents,
                                           tickets: TicketsModel,
                                           groups: GroupsModel,
                                           apps: AppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {


  private val addMemberForm = Form(mapping("email" -> email)(e => e)(Option.apply))

  def addMember(name: String) = Action.async { implicit rq =>
    ???
  }

  def updateMemberForm(name: String, id: Int) = Action.async { implicit rq =>
    ???
  }

  def updateMember(name: String, id: Int) = Action.async { implicit rq =>
    ???
  }

  def deleteMember(name: String, id: Int) = Action.async { implicit rq =>
    ???
  }
}
