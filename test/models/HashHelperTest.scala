package utils

import models.HashModel
import org.specs2.mutable.Specification

/**
  * @author zyuiop
  */
class HashHelperTest extends Specification {

  "HashHelperTest" should {
    "check old passwords" in {
      val password = "abcd1234"
      val hash = "b60367d0f23f4cc5f29fbfcd97d136a2"

      new HashModel().check("old", hash, password) shouldEqual true
      new HashModel().check("old", "112233", password) shouldEqual false
      new HashModel().check("old", hash, "1122333") shouldEqual false
    }

  }
}
