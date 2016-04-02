package wamp.util

import scala.annotation.tailrec

object TopicUtils{

  val TOPIC_DELIM = '.'

  def baseTopicsFromTopic(topic: String): List[String] = {
    val subTopics = topic.split(TOPIC_DELIM).toList
    if(subTopics.size > 1){
      subTopics.drop(1).foldRight( subTopics.take(1) )((item, acc) => acc :+ s"${acc.head}.$item" )
    } else List(topic)
  }


  def wildcardTopicsFromTopic(topic: String) = {

    @tailrec
    def buildTopics(topics: Set[String], topic: String): Set[String] = topic.lastIndexOf('.') match{
      case idx if idx < 0  => topics + s"$topic.*"
      case idx =>
        val prefix = topic.slice(0, idx )
        buildTopics(topics + s"$prefix.*", prefix)
    }

    buildTopics(Set(topic), topic)
  }



}
