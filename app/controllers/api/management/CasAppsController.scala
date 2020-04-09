package controllers.api.management

import java.time.Clock

import ch.japanimpact.api.APIErrorsHelper
import ch.japanimpact.auth.api.apitokens.AuthorizationActions
import ch.japanimpact.auth.api.apitokens.AuthorizationActions.OnlyUsers
import data.CasService
import javax.inject.Inject
import models.ServicesModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class CasAppsController @Inject()(cc: ControllerComponents, authorize: AuthorizationActions, apps: ServicesModel)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) with APIErrorsHelper {

  // TODO: better error handlin

  def getCasServices = authorize(OnlyUsers).async { req =>
    if (req.principal.hasScope("casservices/list")) {
      apps.getCasServices.map(lst => Ok(Json.toJson(lst)))
    } else {
      Future.successful(Unauthorized)
    }
  }

  def getCasService(id: Int) = authorize(OnlyUsers, s"casservices/get/$id").async { req =>
    apps.getServiceById(id).map {
      case Some(app) => Ok(Json.toJson(app))
      case _ => NotFound
    }
  }

  def createCasService = authorize(OnlyUsers, "casservices/create").async(parse.json[CasService]) { req =>
    apps.createApp(req.body.serviceName, req.body.serviceRedirectUrl)
      .map(service => Ok(Json.toJson(service)))
  }

  def updateCasService(id: Int) = authorize(OnlyUsers, s"casservices/update/$id").async(parse.json[CasService]) { req =>
    apps.getCasServiceById(id).flatMap {
      case Some(app) =>
        apps.updateApp(req.body.copy(serviceId = app.serviceId)).map(_ => Ok)
      case None => Future.successful(NotFound)
    }
  }

  def addDomainToWhitelist(id: Int) = authorize(OnlyUsers, s"casservices/domains/add/$id").async(parse.tolerantText(100)) { req =>
    apps.addDomain(id, req.body).map {
      case Some(domain) => Ok(domain)
      case None => APIError(BadRequest, "invalid_domain", "Cannot extract a valid domain name.")
    }
  }

  def removeDomainFromWhitelist(id: Int) = authorize(OnlyUsers, s"casservices/domains/remove/$id").async(parse.tolerantText(100)) { req =>
    apps.removeDomain(id, req.body).map(_ => Ok)
  }

  def addAllowedGroup(id: Int) = authorize(OnlyUsers, s"casservices/domains/add/$id").async(parse.tolerantText(100)) { req =>
    apps.addGroup(id, req.body, false).map(_ => Ok)
  }

  def removeAllowedGroup(id: Int) = authorize(OnlyUsers, s"casservices/domains/remove/$id").async(parse.tolerantText(100)) { req =>
    apps.removeGroup(id, req.body, false).map(_ => Ok)
  }

  def addRequiredGroup(id: Int) = authorize(OnlyUsers, s"casservices/domains/add/$id").async(parse.tolerantText(100)) { req =>
    apps.addGroup(id, req.body, true).map(_ => Ok)
  }

  def removeRequiredGroup(id: Int) = authorize(OnlyUsers, s"casservices/domains/remove/$id").async(parse.tolerantText(100)) { req =>
    apps.removeGroup(id, req.body, true).map(_ => Ok)
  }

  def addAllowedService(id: Int) = authorize(OnlyUsers, s"casservices/domains/add/$id").async(parse.tolerantJson[Int]) { req =>
    apps.addAllowedService(id, req.body).map(_ => Ok)
  }

  def removeAllowedService(id: Int) = authorize(OnlyUsers, s"casservices/domains/remove/$id").async(parse.tolerantJson[Int]) { req =>
    apps.removeAllowedService(id, req.body).map(_ => Ok)
  }

  def deleteApp(id: Int) = authorize(OnlyUsers, s"casservices/delete/$id").async { req =>
    apps.deleteService(id).map(_ => Ok)
  }

}
