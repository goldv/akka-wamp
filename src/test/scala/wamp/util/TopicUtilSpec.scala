package wamp.util

import org.scalatest.{Matchers, FlatSpec}
import wamp.util.TopicUtils

/**
 * Created by goldv on 7/2/2015.
 */
class TopicUtilSpec extends FlatSpec with Matchers{

  it should  "parse non delimited base topic" in{
      assert( TopicUtils.baseTopicsFromTopic("base") ==  List("base") )
    }
  it should "parse delimited base topic" in{
    assert( TopicUtils.baseTopicsFromTopic("base.topic") == List("base", "base.topic") )
  }

  it should "create wildcard based topics from single topic wildcard" in{
    assert(TopicUtils.wildcardTopicsFromTopic("marketdata.fx") == Set("marketdata.*", "marketdata.fx"))
  }

  it should "create wildcard based topics from no topic wildcard" in{
    assert(TopicUtils.wildcardTopicsFromTopic("marketdata") == Set("marketdata", "marketdata.*"))
  }

  it should "create wildcard based topics from multi topic wildcard" in{
    assert(TopicUtils.wildcardTopicsFromTopic("marketdata.fx.eur") == Set("marketdata.*", "marketdata.fx.*", "marketdata.fx.eur"))
  }

}
