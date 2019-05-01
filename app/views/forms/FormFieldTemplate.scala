package views.forms

import views.html

/**
  * @author Louis Vialar
  */
object FormFieldTemplate {
  import views.html.helper.FieldConstructor
  implicit val formFields = FieldConstructor(html.forms.formFieldTemplate.apply)

}
