package com.reactivebbq.loyalty

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.slf4j.LoggerFactory

object Main extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  var port = 0
  val Opt = """-D(\S+)=(\S+)""".r
  args.toList.foreach {
    case Opt(key, value) =>
      log.info(s"Config Override: $key = $value")
      if(key.contains("akka.http.server.default-http-port")) {port = value.toInt}
      System.setProperty(key, value)
  }

  implicit val system: ActorSystem = ActorSystem("Loyalty")

  val rootPath = Paths.get("tmp")
  val loyaltyRepository: LoyaltyRepository = new FileBasedLoyaltyRepository(rootPath)(system.dispatcher)

  val loyaltyActorSupervisor = system.actorOf(LoyaltyActorSupervisor.props(loyaltyRepository))

  // TODO: Uncomment to enable cluster sharding.
  //  val loyaltyActorSupervisor = ClusterSharding(system).start(
  //    "loyalty",
  //    LoyaltyActor.props(loyaltyRepository),
  //    ClusterShardingSettings(system),
  //    LoyaltyActorSupervisor.idExtractor,
  //    LoyaltyActorSupervisor.shardIdExtractor
  //  )

  val loyaltyRoutes = new LoyaltyRoutes(loyaltyActorSupervisor)(system.dispatcher)

  log.info(s"\n =====Bounding to port:======= $port" )
  Http().newServerAt(
    "localhost",
    port
  ).bind(loyaltyRoutes.routes)
}