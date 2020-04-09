package ch.japanimpact.api

import play.api.libs.json.{Format, JsValue, Json, Reads}
import play.api.mvc.{Result, Results}


case class APIError(error: String, errorMessage: String)

object APIError {

  def apply(response: Results.Status, error: String, errorMessage: String): Result = response(Json.toJson(APIError(error, errorMessage)))

  implicit val APIErrorFormat: Format[APIError] = Json.format[APIError]
}

trait APIErrorsHelper extends Results {
  def APIError(response: this.Status, error: String, errorMessage: String): Result = response(Json.toJson(new APIError(error, errorMessage)))
}
