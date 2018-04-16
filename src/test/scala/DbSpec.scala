import java.time.LocalDate

import TickSpecification.userStrategyGen
import cats.effect.IO
import doobie._
import doobie.specs2._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import org.scalacheck.ScalacheckShapeless._
import droletours._

object DbSpec extends Specification with IOChecker {
  val transactor: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:example1", "postgres", "password"
  )


  Tables.tables.foreach(t => check(t.create()))

  implicitly[Arbitrary[ConstantTrigger]]
  val priceGen = implicitly[Arbitrary[PriceTrigger]]
  val trigGen = implicitly[Arbitrary[Trigger]]
  implicitly[Arbitrary[SingleCompanySelector]]
  val secGen = implicitly[Arbitrary[Selector]]

  implicit val userStateGen: Gen[UserState] = for {
    id <- Gen.choose(0, 10)
    str <- userStrategyGen
    nw <- Gen.choose(0.0, 10000.0)
  } yield UserState(List((Nil, str)), nw, nw, LocalDate.now(), LocalDate.now(), id, "name", "a@b.com", true)

  val samplePrice = priceGen.arbitrary.sample.get
  val sampleSe = secGen.arbitrary.sample.get
  val strategy = UserStrategy(samplePrice,samplePrice,sampleSe,1,20,1)
  val exampleUser= userStateGen.sample.get
  val trigger = ConstantTrigger(false)

  //Insert
  check(PriceTrigTable.insert(priceGen.arbitrary.sample.get, strategy))
  check(ConTrigTable.insert(trigger, strategy.copy(buyTrigger = trigger)))

  check(StrategyTable.insert(strategy, exampleUser))

  check(ComSelTable.insert(SingleCompanySelector("goog"), strategy))
  check(SecSelTable.insert(SectorSelector("irrelevant"), strategy))

  check(UserTable.insert(exampleUser))

  //Select

  check(PriceTrigTable.select(1,false))
  check(ConTrigTable.select(1,false))
  check(SecSelTable.select(1))
  check(ComSelTable.select(1))
  check(StrategyTable.selectStrategyData(1))
  check(UserTable.getUserDataById(1))
}
