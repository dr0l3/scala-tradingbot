package droletours

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import cats.data.NonEmptyList
import cats._
import cats.implicits._



case class UserStateV2(strategies: Map[UserStrategy, List[Holding]],
                       capital : Double,
                       netWorth: Option[Double],
                       stateComputedAt: LocalDate,
                       userId: String,
                       displayName: String,
                       email: String
                      )

case class UserState(strategies: List[(List[Holding], UserStrategy)],
                     capital: Double,
                     netWorth: Double,
                     stateComputedAt: LocalDate,
                     insertedAt: LocalDate,
                     userId: Int,
                     displayName: String,
                     email: String,
                     isPlaying: Boolean
                    )

case class UserStrategy(buyTrigger: Trigger,
                        sellTrigger: Trigger,
                        selector: Selector,
                        priority: Short,
                        percentage: Double,
                        strategyId: Int
                      )

object UserStrategy {
  implicit val userStrategyEq = derive.eq[UserStrategy]
}

case class Holding(symbol: String, amount: Double)

case class PriceDataPoint(symbol:String, date: LocalDate, price: Double)


sealed trait Trigger
case class ConstantTrigger(active: Boolean) extends Trigger
case class PriceTrigger(above: Boolean, threshold: Double) extends Trigger

sealed trait Selector
case class SingleCompanySelector(symbol: String) extends Selector
case class SectorSelector(sector: String) extends Selector
