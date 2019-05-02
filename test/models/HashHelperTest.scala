package utils

import org.specs2.mutable.Specification
import services.HashService

/**
  * @author zyuiop
  */
class HashHelperTest extends Specification {

  "HashHelperTest" should {
    "check old passwords" in {
      val password = "abcd1234"
      val hash = "b60367d0f23f4cc5f29fbfcd97d136a2"

      new HashService().check("old", hash, password) shouldEqual true
      new HashService().check("old", "112233", password) shouldEqual false
      new HashService().check("old", hash, "1122333") shouldEqual false
    }

  }
}
