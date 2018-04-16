package integration

import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID

import cats.effect.IO
import doobie.{Query0, Transactor}
import org.scalacheck.Properties
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql._
import droletours._

class InMemoryStuff extends WordSpec with Matchers {
  "A database" can {
    "Add tables" should {
      "and insert data" in {
        val postgres = new EmbeddedPostgres()

        val url = postgres.start("localhost",5431,"example1", "postgres", "password")

        implicit val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
          "org.postgresql.Driver", "jdbc:postgresql://localhost:5431/example1", "postgres", "password"
        )

        Tables.create(xa)

        val netWorth = 10000.0
        val strategies = List(
          UserStrategy(ConstantTrigger(true), ConstantTrigger(false), SingleCompanySelector("GOOG"), 1, 100.0, 1),
          UserStrategy(ConstantTrigger(false), ConstantTrigger(true), SingleCompanySelector("APPL"), 1, 50.0, 2))
        val userState = UserState(strategies.map(str => (Nil, str)), netWorth, netWorth, LocalDate.now(), LocalDate.now(), 1, UUID.randomUUID().toString.substring(0, 10), UUID.randomUUID().toString.substring(0,6) + "@hehe.com", true)

        val res = UserTable.insertUser(userState).unsafeRunSync()

        userState.copy(userId = res.userId) shouldBe res

        import java.time.LocalDate
        import java.util.UUID

        import StrategyTable.insert
        import doobie.free.connection.ConnectionIO
        import doobie._
        import doobie.implicits._
        import cats._
        import cats.data._
        import cats.effect.IO
        import cats.free.Free
        import cats.implicits._
        import doobie.free.connection

        import scala.concurrent.duration._

        def select(strategyId: Int, buyTrigger: Boolean): Query0[PriceTrigger] = {
          sql"""
          SELECT above, threshold FROM price_trigger WHERE strategy_id = ${strategyId} and buy_trigger = ${buyTrigger}
        """.query[PriceTrigger]
        }

        val res2 = select(1231,true).process.take(1).list.transact(xa).unsafeRunSync()

        println(res2)
      }
    }
  }




}

