package utils

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import play.filters.cors.CORSFilter

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

/**
 * Provides filters.
 */
class Filters @Inject() (dbConfigProvider: DatabaseConfigProvider,
  corsFilter: CORSFilter,
  csrfFilter: CSRFFilter,
  securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters  {

  models.DAO.conversion.DatabaseCred.dbConfigTemp = Some ( dbConfigProvider.get[JdbcProfile] )

  override def filters: Seq[EssentialFilter] =  {
    Seq(securityHeadersFilter, corsFilter)
  }
}


import scala.concurrent.Future
import javax.inject._
import play.api.inject.ApplicationLifecycle

@Singleton
class MessageQueueConnection @Inject() (lifecycle: ApplicationLifecycle) {
  //val connection = connectToMessageQueue()
  lifecycle.addStopHook { () =>
  	println("stop hook dbs")
  	println("models.DAO.conversion.DatabaseCred.database.close ")
  	models.DAO.conversion.DatabaseCred.database.close 
  	println("models.DAO.conversion.DatabaseFuture.db.close ")
  	Future.successful(
    models.DAO.conversion.DatabaseFuture.db.close)//connection.stop())
  }

  //...
}