package utils

object CAS {
  private val domainRegex = "^[a-zA-Z0-9_.-]$+".r
  private val serviceRegex = "^https?://([a-zA-Z0-9_.-]+)".r

  def getServiceDomain(service: String) = {
    if (domainRegex.matches(service)) Some(service)
    else serviceRegex.findFirstMatchIn(service).map(r => r.group(1))
  }

}
