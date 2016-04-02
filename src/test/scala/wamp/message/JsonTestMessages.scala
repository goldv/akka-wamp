package wamp.message

import wamp.message.Messages
import Messages.{Welcome, Role, Hello}

/**
 * Created by goldv on 7/1/2015.
 */
trait JsonTestMessages {

  val helloCompleteJson = """
                        | [1,"realm1",{
                        |   "roles":{
                        |     "caller":{"features":{"caller_identification":true,"progressive_call_results":true}},
                        |     "callee":{"features":{"progressive_call_results":true}},
                        |     "publisher":{"features":{"subscriber_blackwhite_listing":true,"publisher_exclusion":true,"publisher_identification":true}},
                        |     "subscriber":{"features":{"publisher_identification":true}}
                        |   }
                        | }]
                      """.stripMargin

  val helloComplete = Hello(
    "realm1",
    List(
      Role("caller",Set("caller_identification", "progressive_call_results")),
      Role("callee",Set("progressive_call_results")),
      Role("publisher",Set("subscriber_blackwhite_listing", "publisher_exclusion", "publisher_identification")),
      Role("subscriber",Set("publisher_identification")) )
  )

  val helloEmptyFeaturesJson =  """
                              | [1,
                              | "realm1",{
                              |   "roles":{
                              |     "caller":{},
                              |     "callee":{},
                              |     "publisher":{},
                              |     "subscriber":{}
                              |   }
                              | }]
                              | """.stripMargin

  val helloEmptyFeatures = Hello("realm1", List(
    Role("caller",Set.empty[String]),
    Role("callee",Set.empty[String]),
    Role("publisher",Set.empty[String]),
    Role("subscriber",Set.empty[String])
  ))

  val welcomeBrokerRoleJson =
    """
      | [2, 0,
      | {
      |   "roles":{
      |     "broker" : {}
      |    }
      | }]
    """.stripMargin

  val welcomeBrokerRole = Welcome(0, List( Role("broker", Set.empty[String])))
}
