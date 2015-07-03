package org.goldv.wampserver.message

import org.goldv.wampserver.message.Messages._
import play.api.libs.json._

/**
 * Created by goldv on 6/30/2015.
 */
trait MessageParser[T]{
  def parse(in: T):  Either[String, WAMPMessage]
  def write(m: WAMPMessage): T
}

class JsonMessageParser extends MessageParser[JsArray] {

  import JsonMessageParser._

  def parse(arr: JsArray): Either[String, WAMPMessage] = {
    val result = for{
      messageType <- arr.transform(messageTypeTransformer)
      wampMessage <- parseType(messageType.value.intValue(), arr)
    } yield wampMessage

    result match{
      case JsSuccess(message, _) => Right(message)
      case JsError(err) => Left(s"Unable to retrieve message type id from ${arr} error: ${err}")
    }
  }

  def write(msg: WAMPMessage) = msg match{
    case m:Welcome => Json.toJson(m).as[JsArray]
    case s:Subscribed => Json.toJson(s).as[JsArray]
    case e:Event => Json.toJson(e).as[JsArray]
    case g:Goodbye => Json.toJson(g).as[JsArray]
    case _ => JsArray()
  }

  def parseType(messageType: Int, arr: JsArray): JsResult[WAMPMessage] = messageType match{
    case Messages.HELLO_TYPE => parseHello(arr)
    case Messages.WELCOME_TYPE => parseWelcome(arr)
    case Messages.SUBSCRIBE_TYPE => parseSubscribe(arr)
    case Messages.UNSUBSCRIBE_TYPE => parseUnsubscribe(arr)
    case Messages.GOODBYE_TYPE => parseGoodbye(arr)
    case _ => JsError("Unknown message type")
  }

  def parseHello(arr: JsArray): JsResult[Hello] = for{
    realm <- arr.transform[JsString]( __(1).json.pick[JsString] )
    details <- arr.transform[JsObject]( __(2).json.pick[JsObject] )
    roles <- parseRoles(details)
  } yield Hello(realm.value, roles)

  def parseGoodbye(arr: JsArray): JsResult[Goodbye] = for{
    reason <- arr.transform[JsString]( __(2).json.pick[JsString] )
  } yield Goodbye(reason.value)

  def parseSubscribe(arr: JsArray) = for{
    subscriptionId <- arr.transform[JsNumber](__(1).json.pick[JsNumber])
    options <- arr.transform[JsObject](__(2).json.pick[JsObject]) // ignoring for now
    topic <- arr.transform[JsString](__(3).json.pick[JsString])
  } yield Subscribe(subscriptionId.value.toLongExact, topic.value.toString)

  def parseUnsubscribe(arr: JsArray) = for{
    id <- arr.transform[JsNumber](__(1).json.pick[JsNumber])
    subId <- arr.transform[JsNumber](__(2).json.pick[JsNumber])
  } yield Unsubscribe(id.value.toLong, subId.value.toLong)

  def parseWelcome(arr: JsArray): JsResult[Welcome] =  for{
    session <- arr.transform[JsNumber]( __(1).json.pick[JsNumber] )
    details <- arr.transform[JsObject]( __(2).json.pick[JsObject] )
    roles <- parseRoles(details)
  } yield Welcome(session.value.toLongExact, roles)


  def parseRoles(obj: JsObject): JsResult[List[Role]] = for {
    rolesBranch <- obj.validate[JsObject]( (__ \ "roles").json.pick[JsObject] )
  } yield parseFeatures(rolesBranch).toList

  def parseFeatures(obj: JsObject) = obj.value.collect{ case (role: String, features: JsObject) => Role(role, parseFeatureMap( features ))}

  def parseFeatureMap(featureObj: JsObject) = featureObj.validate[JsObject]((__ \ "features").json.pick[JsObject] ) match {
    case JsSuccess(features, _ ) => features.value.collect{ case (feature: String, JsBoolean(true)) => feature}.toSet
    case JsError(err) => Set.empty[String]
  }
}

object JsonMessageParser{

  val messageTypeTransformer = __(0).json.pick[JsNumber]

  implicit val roleWrite: Writes[Role] = new Writes[Role]{
    override def writes(r: Role): JsValue = {
      val features = r.features.foldRight(Json.obj())( (c, acc) => acc ++ Json.obj( "features" -> Json.obj( c -> JsBoolean(true) ) ))
      Json.obj(
        r.name -> features
      )
    }
  }

  implicit val welcomeWrites: Writes[Welcome] = new Writes[Welcome] {
    override def writes(o: Welcome): JsValue = {
      val details = Json.obj( "roles" -> o.details.foldRight(Json.obj())((role, acc) => acc ++ Json.toJson(role).as[JsObject] ) )
      JsArray( Seq(JsNumber(Messages.WELCOME_TYPE), JsNumber(o.session), details) )
    }
  }

  implicit val subscribedWrites: Writes[Subscribed] = new Writes[Subscribed] {
    override def writes(s: Subscribed): JsValue = JsArray( Seq(JsNumber(Messages.SUBSCRIBED_TYPE), JsNumber(s.id), JsNumber(s.brokerId) ) )
  }

  implicit val unsubscribedWrites: Writes[Unsubscribed] = new Writes[Unsubscribed] {
    override def writes(u: Unsubscribed): JsValue = JsArray( Seq(JsNumber(Messages.UNSUBSCRIBED_TYPE), JsNumber(u.id)))
  }

  implicit val eventWrites: Writes[Event] = new Writes[Event]{
    override def writes(e: Event): JsValue = JsArray( Seq(JsNumber(Messages.EVENT_TYPE), JsNumber(e.subId), JsNumber(e.pubId), Json.obj(), e.payload ))
  }

  implicit val goodbyeWrites: Writes[Goodbye] = new Writes[Goodbye]{
    override def writes(g: Goodbye): JsValue = JsArray( Seq(JsNumber(Messages.GOODBYE_TYPE), Json.obj(), JsString(g.reason) ) )
  }
}
