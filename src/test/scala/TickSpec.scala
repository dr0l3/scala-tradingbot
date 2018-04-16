import java.time.LocalDate

import cats._
import cats.data._
import cats.implicits._
import org.scalacheck.Prop._
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalatest.{Matchers, WordSpec}
import droletours._

class TickSpec extends WordSpec with Matchers {
  "Calculated netwroth" should {
    "have correct amount of strategies and networth" in {
      val netWorth = 10000.0
      val prices = Map("GOOG" -> PriceDataPoint("GOOG", LocalDate.now(), 100.0))
      val strategy = UserStrategy(ConstantTrigger(true), ConstantTrigger(false), SingleCompanySelector("GOOG"), 1, 100.0,1)
      val userState = UserState(List((Nil, strategy)), netWorth, netWorth, LocalDate.now(), LocalDate.now(),1, "name", "a@b.com", true)

      val next = new Ticker().calculateHoldings(userState, prices)

      next.strategies.size shouldBe 1
      RandomUtils.valueOfHoldings(next.strategies.flatMap(p => p._1), prices) shouldBe netWorth
    }
  }

  "Calculated networth" should {
    "have correct amount of strateiges and networth" in {
      val netWorth = 10000.0

      val prices = Map("GOOG" -> PriceDataPoint("GOOG", LocalDate.now(), 100.0), "MSFT" -> PriceDataPoint("MSFT", LocalDate.now(), 12.0))
      val msftStrategy = UserStrategy(PriceTrigger(above = true, 10.0), ConstantTrigger(false), SingleCompanySelector("MSFT"), 1, 50.0,1)
      val googleStrategy = UserStrategy(ConstantTrigger(true), PriceTrigger(above = true, 95.0), SingleCompanySelector("GOOG"),1,50.0, 2)
      val userState = UserState(List((Nil, googleStrategy), (Nil, msftStrategy)), netWorth, netWorth, LocalDate.now(), LocalDate.now(), 3, "name", "a@b.com", true)

      val next = new Ticker().calculateHoldings(userState, prices)

      next.strategies.size shouldBe userState.strategies.size
      RandomUtils.valueOfHoldings(next.strategies.flatMap(p => p._1), prices) shouldBe netWorth
    }
  }

}


object TickSpecification extends Properties("Tick props") {

  implicitly[Arbitrary[ConstantTrigger]]
  implicitly[Arbitrary[PriceTrigger]]
  implicitly[Arbitrary[Trigger]]
  implicitly[Arbitrary[SingleCompanySelector]]
  implicitly[Arbitrary[Selector]]


  val dateGen = for {
    month <- Gen.oneOf(1, 12)
    day <- Gen.oneOf(1, 28)
    year <- Gen.oneOf(2000, 2012)
  } yield LocalDate.of(year, month, day)

  val triggerGen: Gen[Trigger] = implicitly[Arbitrary[Trigger]].arbitrary

  val userStrategyGen: Gen[UserStrategy] = for {
    id <- Gen.choose(0,10)
    symbol <- Gen.alphaStr
    sellTrigger <- implicitly[Arbitrary[Trigger]].arbitrary
    buyTrigger <- implicitly[Arbitrary[Trigger]].arbitrary
  } yield UserStrategy(buyTrigger, sellTrigger, SingleCompanySelector(symbol), 1, 100, id)


  implicit val userStateGen: Gen[UserState] = for {
    id <- Gen.choose(0, 10)
    str <- userStrategyGen
    nw <- Gen.choose(0.0, 10000.0)
  } yield UserState(List((Nil, str)), nw, nw, LocalDate.now(), LocalDate.now(), id, "name", "a@b.com", true)

  val priceGen: Gen[PriceDataPoint] = for {
    sy <- Gen.alphaUpperStr
    pr <- Gen.choose(1000.0, 10000.0)
  } yield PriceDataPoint(sy, LocalDate.now(), pr)

  val priceMapGen: Gen[Map[String, PriceDataPoint]] = for {
    prices <- Gen.listOfN(100, priceGen)
  } yield prices.map(p => p.symbol -> p).toMap


  property("conservation of networth and strategies") = forAll(userStateGen, priceMapGen) { (userState: UserState, prices: Map[String, PriceDataPoint]) =>
    val nextState = new Ticker().calculateHoldings(userState, prices)
    val nextHoldings = nextState.strategies.flatMap(p => p._1)
    val valueOfHoldings = RandomUtils.valueOfHoldings(nextHoldings, prices)
    userState.netWorth === valueOfHoldings + nextState.capital
    userState.strategies.map(_._2) === nextState.strategies.map(_._2)
  }
}