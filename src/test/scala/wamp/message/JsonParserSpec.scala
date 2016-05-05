package wamp.message

import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsonParser

import scala.util.Success

/**
 * Created by goldv on 7/1/2015.
 */
class JsonParserSpec extends FlatSpec with Matchers with JsonTestMessages{

  val parser = MessageParser

  "hello parser" should "parse hello message with populated roles" in{
      assert( MessageParser.parse(JsonParser(helloCompleteJson)) ==  Success(helloComplete) )
  }
  "hello parser" should  "parse hello with empty features" in {
      assert( MessageParser.parse(JsonParser(helloEmptyFeaturesJson)) == Success( helloEmptyFeatures ) )
  }
  "hello parser" should "write welcome json message" in{
      assert( Success(welcomeBrokerRole) ==  MessageParser.parse(JsonParser(welcomeBrokerRoleJson)) )
  }

}
