package com.goldv.wampserver.util

import org.goldv.wampserver.server.TopicUtils
import org.specs2.mutable.Specification

/**
 * Created by goldv on 7/2/2015.
 */
class TopicUtilSpec extends Specification{

  "topic utils" should{
    "parse non delimited base topic" in{
      TopicUtils.baseTopicsFromTopic("base") must beEqualTo(List("base"))
    }
    "parse delimited base topic" in{
      TopicUtils.baseTopicsFromTopic("base.topic") must beEqualTo(List("base", "base.topic"))
    }
  }

}
