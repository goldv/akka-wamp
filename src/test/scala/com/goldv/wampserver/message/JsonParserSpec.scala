package com.goldv.wampserver.message

import org.goldv.wampserver.message.JsonMessageParser
import org.goldv.wampserver.message.Messages.{Hello, Role}
import org.specs2.mutable.Specification
import play.api.libs.json.{JsArray, Json}

/**
 * Created by goldv on 7/1/2015.
 */
class JsonParserSpec extends Specification with JsonTestMessages{

  import JsonMessageParser._

  val parser = new JsonMessageParser

  "hello parser" should{
    "parse hello message with populated roles" in{
      parser.parse(Json.parse(helloCompleteJson).as[JsArray]) must beRight( helloComplete )
    }
    "parse hello with empty features" in {
      parser.parse(Json.parse(helloEmptyFeaturesJson).as[JsArray]) must beRight( helloEmptyFeatures )
    }
    "write welcome json message" in{
      Json.toJson(welcomeBrokerRole) must beEqualTo( Json.parse(welcomeBrokerRoleJson) )
    }
  }

}
