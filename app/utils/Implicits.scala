package utils

import constants.GeneralErrorCodes.error
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * @author zyuiop
  */
object Implicits {

  /**
    * This class allows to use `.asFuture` to convert any value to a future
    */
  implicit class PostFixAsFuture[T](value: T) {
    implicit def asFuture: Future[T] = Future.successful(value)
  }

  /**
    * This implicit conversion allows to convert implicitly any value to a future
    *
    * @param x the value to convert
    */
  implicit def toFuture[A](x: => A)(implicit ec: ExecutionContext): Future[A] = Future(x)

  /**
    * This implicit conversion allows to convert implicitly any Json writable value to an Ok result
    * @param x the value to convert
    */
  implicit def toOkResult[A](x: A)(implicit writes: Writes[A]): Result = Results.Ok(Json.toJson(x))


  /**
    * This implicit class allows to use `! code` to return an error code (`BadRequest(RequestError(code))`)
    */
  implicit class AddUnaryErrorCode(val code: Int) extends AnyVal {
    def unary_! : Result = error(code)
  }

}
