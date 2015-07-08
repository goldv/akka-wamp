package com.goldv.wampserver.message

import org.goldv.wampserver.message.JsonMessageParser
import org.goldv.wampserver.message.Messages.{Hello, Role}
import play.api.libs.json.{JsArray, Json}
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by goldv on 7/1/2015.
 */
class JsonParserSpec extends FlatSpec with Matchers with JsonTestMessages{

  import JsonMessageParser._

  val parser = new JsonMessageParser

  "hello parser" should "parse hello message with populated roles" in{
      assert( parser.parse(Json.parse(helloCompleteJson).as[JsArray]) ==  Right(helloComplete) )
  }
  "hello parser" should  "parse hello with empty features" in {
      assert( parser.parse(Json.parse(helloEmptyFeaturesJson).as[JsArray]) == Right( helloEmptyFeatures ) )
  }
  "hello parser" should "write welcome json message" in{
      assert( Json.toJson(welcomeBrokerRole) ==  Json.parse(welcomeBrokerRoleJson) )
  }

}
