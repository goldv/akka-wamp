package com.goldv.wampserver.util

import org.goldv.wampserver.server.TopicUtils
import org.scalatest.{Matchers, FlatSpec}

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


}
