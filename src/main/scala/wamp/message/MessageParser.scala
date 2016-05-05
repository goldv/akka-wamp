package wamp.message

import spray.json._
import wamp.message.Messages._

import scala.util.{Failure, Success, Try}

/**
 * Created by goldv on 6/30/2015.
 */


object MessageParser{

  def parse(value: JsValue) = value match{
    case arr:JsArray => parseArray(arr)
    case _ => Failure(new Exception(s"Unable to parse message $value"))
  }

  def parseArray(arr: JsArray): Try[WAMPMessage] = arr.elements match{
    case Vector(Messages.HELLO_TYPE, JsString(realm), roles:JsObject ) => Success(Hello(realm, parseFeatures(roles.fields("roles").asJsObject.fields)))
    case Vector(Messages.WELCOME_TYPE, JsNumber(session), roles:JsObject) => Success(Welcome(session.toLongExact, parseFeatures(roles.fields("roles").asJsObject.fields)))
    case Vector(Messages.SUBSCRIBE_TYPE, JsNumber(id), _ , JsString(topic)) => Success(Subscribe(id.toLongExact, topic))
    case Vector(Messages.UNSUBSCRIBE_TYPE, JsNumber(id), JsNumber(subId)) => Success(Unsubscribe(id.toLongExact, subId.toLongExact))
    case Vector(Messages.GOODBYE_TYPE, _ , JsString(reason)) => Success(Goodbye(reason))
    case _ => Failure(new Exception(s"Unable to parse message $arr"))
  }

  def write(msg: WAMPMessage) = msg match{
    case m:Welcome => m.toJson
    case s:Subscribed => s.toJson
    case e:Event => e.toJson
    case g:Goodbye => g.toJson
    case _ => throw new RuntimeException(s"Unsupported message type $msg")
  }

  def parseFeatures(fields: Map[String, JsValue]) = fields.toList.collect{ case (role: String, features: JsObject) => Role(role, parseFeatureMap( features.fields ))}

  def parseFeatureMap(featureObj: Map[String,JsValue]) = featureObj.get("features") match {
    case Some(JsObject((features) )) => features.toList.collect{ case (feature: String, JsBoolean(true)) => feature}.toSet
    case _ => Set.empty[String]
  }

  implicit val roleWrite: RootJsonWriter[Role] = new RootJsonWriter[Role]{
    override def write(r: Role): JsValue = {
      val features = r.features.foldRight(JsObject())( (c, acc) => acc.copy( fields = acc.fields + ("features" -> JsObject( c -> JsBoolean(true) ))) )
      JsObject(r.name -> features)
    }
  }

  implicit val welcomeWrites: RootJsonWriter[Welcome] = new RootJsonWriter[Welcome] {
    override def write(o: Welcome): JsValue = {
      val details = JsObject( "roles" -> o.details.foldRight(JsObject())((role, acc) => JsObject(acc.fields.toList ++ role.toJson.asJsObject.fields.toList:_*) ) )
      JsArray( Vector(Messages.WELCOME_TYPE, JsNumber(o.session), details) )
    }
  }

  implicit val subscribedWrites: RootJsonWriter[Subscribed] = new RootJsonWriter[Subscribed] {
    override def write(s: Subscribed): JsValue = JsArray( Vector(Messages.SUBSCRIBED_TYPE, JsNumber(s.id), JsNumber(s.brokerId) ) )
  }

  implicit val unsubscribedWrites: RootJsonWriter[Unsubscribed] = new RootJsonWriter[Unsubscribed] {
    override def write(u: Unsubscribed): JsValue = JsArray( Vector(Messages.UNSUBSCRIBED_TYPE, JsNumber(u.id)))
  }

  implicit val eventWrites: RootJsonWriter[Event] = new RootJsonWriter[Event]{
    override def write(e: Event): JsValue = JsArray( List(Messages.EVENT_TYPE, JsNumber(e.subId), JsNumber(e.pubId), JsObject(), e.payload ))
  }

  implicit val goodbyeWrites: RootJsonWriter[Goodbye] = new RootJsonWriter[Goodbye]{
    override def write(g: Goodbye): JsValue = JsArray( Vector(Messages.GOODBYE_TYPE, JsObject(), JsString(g.reason) ) )
  }
}
