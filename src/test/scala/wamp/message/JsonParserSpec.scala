package wamp.message

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsArray, Json}
import wamp.message.MessageParser

import scala.util.Success

/**
 * Created by goldv on 7/1/2015.
 */
class JsonParserSpec extends FlatSpec with Matchers with JsonTestMessages{

  import MessageParser._

  val parser = MessageParser

  "hello parser" should "parse hello message with populated roles" in{
      assert( MessageParser.parse(Json.parse(helloCompleteJson).as[JsArray]) ==  Success(helloComplete) )
  }
  "hello parser" should  "parse hello with empty features" in {
      assert( MessageParser.parse(Json.parse(helloEmptyFeaturesJson).as[JsArray]) == Success( helloEmptyFeatures ) )
  }
  "hello parser" should "write welcome json message" in{
      assert( Json.toJson(welcomeBrokerRole) ==  Json.parse(welcomeBrokerRoleJson) )
  }

}
