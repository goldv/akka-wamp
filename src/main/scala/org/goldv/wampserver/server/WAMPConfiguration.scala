package org.goldv.wampserver.server

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

/**
 * Created by goldv on 7/3/2015.
 */
trait WAMPConfiguration {

  lazy val apiExecutionContext = ExecutionContext.fromExecutor( Executors.newCachedThreadPool() )

}
