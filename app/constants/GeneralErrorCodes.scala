package constants

/**
  * @author Louis Vialar
  */
object GeneralErrorCodes {
  /**
    * Some data in the request is missing or invalid
    */
  val MissingData = 101

  /**
    * The requested app was not found
    */
  val UnknownApp = 102

  /**
    * The requested app was found but the App Secret was incorrect
    */
  val InvalidAppSecret = 103
}
