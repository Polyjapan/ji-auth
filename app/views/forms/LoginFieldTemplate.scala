package views.forms

import views.html

/**
  * @author Louis Vialar
  */
object LoginFieldTemplate {
  import views.html.helper.FieldConstructor
  implicit val loginFields = FieldConstructor(html.forms.loginFormTemplate.apply)

}
