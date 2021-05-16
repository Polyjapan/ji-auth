package controllers

package object saml2 {
  object SAMLBindings {
    val HTTPRedirect = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
    val HTTPPost = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
  }

  object SAMLNameIdFormats {
    val EmailAddressFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
  }
}
