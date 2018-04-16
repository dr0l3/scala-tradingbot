package droletours

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

object TryThings extends App {
  implicit val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:example1", "postgres", "password"
  )

  Tables.create(xa)
  //
  //  UserTable.create().run.transact(xa).unsafeRunSync()
  //  StrategyTable.create().run.transact(xa).unsafeRunSync()
  //  ComSelTable.create().run.transact(xa).unsafeRunSync()

  val netWorth = 10000.0
  val prices = Map("GOOG" -> PriceDataPoint("GOOG", LocalDate.now(), 100.0))
  val strategies = List(
    UserStrategy(ConstantTrigger(true), ConstantTrigger(false), SingleCompanySelector("GOOG"), 1, 100.0, 1),
    UserStrategy(ConstantTrigger(false), ConstantTrigger(true), SingleCompanySelector("APPL"), 1, 50.0, 2))
  val userState = UserState(strategies.map(str => (Nil, str)), netWorth, netWorth, LocalDate.now(), LocalDate.now(), 1, UUID.randomUUID().toString.substring(0, 10), UUID.randomUUID().toString.substring(0, 6) + "@hehe.com", true)

  val start = System.nanoTime()
  val res = UserTable.insertUser(userState).unsafeRunSync()
  val total = System.nanoTime() - start
  val milis = MILLISECONDS.convert(total, NANOSECONDS)

  println(s"Total time to insert: ${milis}")
  println(res)
}

object Testy extends App {
  val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:example1", "postgres", "password"
  )

}

object Database extends App {

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:", "postgres", "password"
  )

  val program1: ConnectionIO[List[(String, String, String)]] = sql"SELECT * FROM country"
    .query[(String, String, String)]
    .list

  println(program1)

  val task = program1.transact(xa)

  val res = task.unsafeRunSync()

  println(res)

  case class Country(code: String, name: String, population: Int)

  def insert(country: Country): Update0 = {
    sql"INSERT INTO country(code, name, population) VALUES (${country.code}, ${country.name},${country.population})".update
  }

  val a = insert(Country("NOO", "blah", 42)).run.transact(xa)
  a.unsafeRunSync

}

sealed trait DbTable {
  def create(): Update0
}

object Tables {
  val tables = List(UserTable, StrategyTable, ComSelTable, SecSelTable, ConTrigTable, PriceTrigTable)

  def create(xa: Transactor.Aux[IO, Unit]) = {
    tables.foreach(t => t.create().run.transact(xa).unsafeRunSync)
  }

  //  def check(xa: Transactor.Aux[IO, Unit]) = {
  //    tables.foreach(t => t.create().check.unsafePerformIO)
  //  }
}

object UserTable extends DbTable {
  def create(): Update0 = {
    sql"""
          CREATE TABLE IF NOT EXISTS user_data (
            user_id SERIAL PRIMARY KEY UNIQUE,
            name VARCHAR(50) NOT NULL UNIQUE,
            email VARCHAR(50) NOT NULL UNIQUE,
            inserted_at DATE NOT NULL,
            playing BOOLEAN NOT NULL
          )
      """.update
  }

  def insert(user: UserState): Update0 = {
    sql"""
          INSERT INTO user_data (user_id, name, email, inserted_at, playing)
          VALUES (DEFAULT, ${user.displayName}, ${user.email}, ${user.insertedAt},${user.isPlaying})
       """.update
  }

  def insertUser(user: UserState)(implicit xa: Transactor.Aux[IO, Unit]) = {
    val trans: Free[connection.ConnectionOp, UserState] = for {
      userId <- insert(user).withUniqueGeneratedKeys[Int]("user_id")
      userWithId = user.copy(userId = userId)
      _ <- insertStrategies(userWithId.strategies.map(_._2), userWithId)
    } yield userWithId

    trans.transact(xa)
  }

  def insertStrategies(strategies: List[UserStrategy], user: UserState): Free[connection.ConnectionOp, Unit] = {
    val progs: List[Free[connection.ConnectionOp, Unit]] = strategies.map(strategy => {
      for {
        id <- StrategyTable.insert(strategy, user).withUniqueGeneratedKeys[Int]("strategy_id")
        strategyWithId = strategy.copy(strategyId = id)
        _ <- insertSelector(strategy.selector, strategyWithId).run
        _ <- insertTrigger(strategy.buyTrigger, strategyWithId).run
        _ <- insertTrigger(strategy.sellTrigger, strategyWithId).run
      } yield ()
    })

    val noop: Free[connection.ConnectionOp, Unit] = for {
      _ <- sql"".update.run
    } yield ()

    progs.foldLeft(noop)((acc, next) => acc.flatMap(_ => next))

  }

  private def insertSelector(selector: Selector, strategy: UserStrategy) = {
    selector match {
      case scSel: SingleCompanySelector => ComSelTable.insert(scSel, strategy)
      case secSel: SectorSelector => SecSelTable.insert(secSel, strategy)
    }
  }

  private def insertTrigger(trigger: Trigger, strategy: UserStrategy) = {
    trigger match {
      case c: ConstantTrigger => ConTrigTable.insert(c, strategy)
      case p: PriceTrigger => PriceTrigTable.insert(p, strategy)
    }
  }

  def getUserById(userId: Int) = {
//    val prog = for {
//      userdata <- getUserDataById(userId)
//      strategies <- StrategyTable.selectStrategyData(userId)
//    } yield strategies
  }

  def getUserDataById(userId: Int): Query0[(Int,String,String,LocalDate,Boolean)] = {
    sql"""Select user_id,name,email,inserted_at,playing FROM user_data WHERE user_id = ${userId}""".query[(Int,String,String,LocalDate,Boolean)]
  }

  def fetchDataForStrategy(data: (Int, Double, Short)) = {

  }

  def trigger(strategyId: Int, buyTrigger: Boolean) = {

//    val prog: Free[connection.ConnectionOp, Trigger] = for {
//      pTrig <- PriceTrigTable.select(strategyId,buyTrigger).process.
//      pTrig
//      len = pTrig.size
//      res: List[Trigger] = if(len > 0) ConTrigTable.select(strategyId, buyTrigger).process.list else pTrig
//    } yield res

//    val res = prog.run

  }

}

  object StrategyTable extends DbTable {
    def create(): Update0 = {
      sql"""
          CREATE TABLE IF NOT EXISTS strategy_data (
            strategy_id SERIAL PRIMARY KEY UNIQUE,
            user_id INTEGER REFERENCES user_data(user_id),
            percentage FLOAT NOT NULL,
            priority SMALLINT NOT NULL
          )
      """.update
    }

    def insert(strategy: UserStrategy, user: UserState): Update0 = {
      sql"""
          INSERT INTO strategy_data (strategy_id, user_id, percentage, priority)
          VALUES ( DEFAULT,${user.userId}, ${strategy.percentage}, ${strategy.priority})
          RETURNING strategy_id
       """.update
    }

    def selectStrategyData(userId: Int): Query0[(Int, Double, Short)] = {
      sql"""
         SELECT strategy_id,percentage, priority from strategy_data where user_id = ${userId}
         """.query[(Int, Double, Short)]
    }

  }

  object ComSelTable extends DbTable {
    def create(): Update0 = {
      sql"""
         CREATE TABLE IF NOT EXISTS company_selector (
          strategy_id INTEGER REFERENCES strategy_data(strategy_id),
          symbol VARCHAR(5) NOT NULL
         )
       """.update
    }

    def insert(selector: SingleCompanySelector, strategy: UserStrategy): Update0 = {
      sql"""
          INSERT INTO company_selector (strategy_id, symbol) VALUES (${strategy.strategyId}, ${selector.symbol})
       """.update
    }

    def select(strategyId: Int): Query0[SingleCompanySelector] = {
      sql"""
          SELECT symbol FROM company_selector WHERE strategy_id = ${strategyId}
        """.query[SingleCompanySelector]
    }

  }

  object SecSelTable extends DbTable {
    def create(): Update0 = {
      sql"""
         CREATE TABLE IF NOT EXISTS sector_selector (
          strategy_id INTEGER REFERENCES strategy_data(strategy_id),
          sector VARCHAR(20) NOT NULL
         )
       """.update
    }

    def insert(selector: SectorSelector, strategy: UserStrategy): Update0 = {
      sql"""
          INSERT INTO sector_selector (strategy_id, sector) VALUES (${strategy.strategyId}, ${selector.sector})
       """.update
    }

    def select(strategyId: Int): Query0[SectorSelector] = {
      sql"""
          SELECT sector FROM sector_selector WHERE strategy_id = ${strategyId}
        """.query[SectorSelector]
    }
  }

  object ConTrigTable extends DbTable {
    def create(): Update0 = {
      sql"""
         CREATE TABLE IF NOT EXISTS constant_trigger (
          strategy_id INTEGER REFERENCES strategy_data(strategy_id),
          active BOOLEAN NOT NULL,
          buy_trigger BOOLEAN NOT NULL
         )
       """.update
    }

    def insert(trigger: ConstantTrigger, strategy: UserStrategy): Update0 = {
      sql"""
          INSERT INTO constant_trigger (strategy_id, active, buy_trigger)
          VALUES (${strategy.strategyId},${trigger.active}, ${trigger == strategy.buyTrigger})
       """.update
    }

    def select(strategyId: Int, buyTrigger: Boolean): Query0[ConstantTrigger] = {
      sql"""
          SELECT active FROM constant_trigger WHERE strategy_id = ${strategyId} and buy_trigger = ${buyTrigger}
        """.query[ConstantTrigger]
    }

  }

  object PriceTrigTable extends DbTable {
    def create(): Update0 = {
      sql"""
         CREATE TABLE IF NOT EXISTS price_trigger (
          strategy_id INTEGER REFERENCES strategy_data(strategy_id),
          above BOOLEAN NOT NULL,
          threshold FLOAT NOT NULL,
          buy_trigger BOOLEAN NOT NULL
         )
       """.update
    }

    def insert(trigger: PriceTrigger, strategy: UserStrategy): Update0 = {
      sql"""
       INSERT INTO price_trigger (strategy_id, above, threshold, buy_trigger)
       VALUES (${strategy.strategyId}, ${trigger.above}, ${trigger.threshold}, ${trigger == strategy.buyTrigger})
      """.update
    }

    def select(strategyId: Int, buyTrigger: Boolean): Query0[PriceTrigger] = {
      sql"""
          SELECT above, threshold FROM price_trigger WHERE strategy_id = ${strategyId} and buy_trigger = ${buyTrigger}
        """.query[PriceTrigger]
    }
  }

  object SimpleAccessPatterns {


    //  def insertData(a: U) = {
    //    sql"""
    //          INSERT INTO
    //      """.update
    //  }
  }
