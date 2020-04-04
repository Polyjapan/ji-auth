package ch.japanimpact.auth.api.apitokens

import ch.japanimpact.auth.api.apitokens.AuthorizationActions.PrincipalFilter
import javax.inject.Inject
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class AuthorizationActions @Inject()(val parser: BodyParsers.Default, jwt: APITokensValidationService)(implicit val executionContext: ExecutionContext) {

  class AuthorizationActionBuilder(requiredScopes: Set[String], principalValidator: PrincipalFilter = _ => true) extends ActionBuilder[AuthorizedRequest, AnyContent]
    with ActionRefiner[Request, AuthorizedRequest] {
    override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorizedRequest[A]]] = Future.successful {
      val principal = request.headers.get("Authorization").filter(_.startsWith("Bearer"))
        .map(_.replace("Bearer ", "").trim)
        .flatMap(jwt.validateToken)

      principal match {
        case None =>
          // No principal in the request
          Left(Unauthorized)

        case Some(user) if (requiredScopes.forall(user.hasScope) && principalValidator(user.principal)) =>
          // Principal with access to the scope
          Right(new AuthorizedRequest(user, request))

        case _ =>
          // Principal with no access to the scope
          Left(Forbidden)
      }
    }

    override def parser: BodyParser[AnyContent] = AuthorizationActions.this.parser

    override protected def executionContext: ExecutionContext = AuthorizationActions.this.executionContext
  }

  class AuthorizedRequest[A](val principal: AuthentifiedPrincipal, request: Request[A]) extends WrappedRequest[A](request)

  /**
    * Create an action that only ensures a valid token is present
    */
  def apply() = new AuthorizationActionBuilder(Set.empty)

  /**
    * Create an action that ensures that a valid token is present and that it contains specific scope(s)
    */
  def apply(requiredScopes: String*) = new AuthorizationActionBuilder(Set(requiredScopes))

  /**
    * Create an action that ensures that a valid token is present, that it contains specific scope(s), and that it
    * matches a condition regarding its principal (i.e. bot only)
    */
  def apply(principalFilter: PrincipalFilter, requiredScopes: String*) = new AuthorizationActionBuilder(Set(requiredScopes), principalFilter)

  /**
    * Create an action that ensures that a valid token is present and that its principal matches a condition
    */
  def apply(principalFilter: PrincipalFilter) = new AuthorizationActionBuilder(Set.empty, principalFilter)
}

object AuthorizationActions {
  type PrincipalFilter = Principal => Boolean

  val OnlyApps: PrincipalFilter = {
    case App(_) => true
    case _ => false
  }

  val OnlyUsers: PrincipalFilter = {
    case User(_) => true
    case _ => false
  }
}