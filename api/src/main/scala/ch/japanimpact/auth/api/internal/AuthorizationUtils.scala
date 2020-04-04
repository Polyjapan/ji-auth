package ch.japanimpact.auth.api.internal

import ch.japanimpact.auth.api.AuthorizedUser
import javax.inject.Inject
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Utility class to remove some boilerplate regarding authentication on the endpoints
  *
  * @author Louis Vialar
  */
object AuthorizationUtils {
  class UserRequest[A](val user: Option[AuthorizedUser], request: Request[A]) extends WrappedRequest[A](request)

  class UserAction @Inject()(val parser: BodyParsers.Default, jwt: TokenValidationService)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent]
      with ActionTransformer[Request, UserRequest] {
    def transform[A](request: Request[A]) = Future.successful {
      val token = request.headers.get("Authorization")
        .filter(_.startsWith("Bearer"))
        .map(_.replace("Bearer ", "").trim)

      new UserRequest[A](token.flatMap(jwt.validateToken), request)
    }
  }

  def PermissionCheckAction(requiredGroups: Set[String] = Set.empty)(implicit ec: ExecutionContext): ActionFilter[UserRequest] = new ActionFilter[UserRequest] {
    def executionContext = ec

    def filter[A](input: UserRequest[A]) = Future.successful {
      if (input.user.isEmpty) {
        Some(Unauthorized)
      } else if ((requiredGroups -- input.user.get.groups).nonEmpty) {
        Some(Forbidden)
      } else {
        None // Request can continue
      }
    }
  }
}
