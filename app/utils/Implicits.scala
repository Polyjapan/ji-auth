package utils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * @author zyuiop
  */
object Implicits {

  implicit class PostFixAsFuture[T](value: T) {
    implicit def asFuture: Future[T] = Future.successful(value)
  }

  implicit def toFuture[A](x: => A)(implicit ec: ExecutionContext): Future[A] = Future(x)

}
