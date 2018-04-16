package droletours

object SymbolResolver {
  def resolveSymbols(selector: Selector): List[String] = {
    selector match {
      case SingleCompanySelector(symbol) => List(symbol)
      case SectorSelector(sector) => ???
    }
  }
}


object TriggerUtils {

  def activeSymbols(selector: Selector, buyTrigger: Trigger, sellTrigger: Trigger, prices: Map[String,PriceDataPoint]): List[String] = {
    val symbols = SymbolResolver.resolveSymbols(selector)
    symbols.filter(symbol => isSymbolActive(symbol, buyTrigger, sellTrigger, prices))
  }

  def isSymbolActive(symbol: String, buyTrigger: Trigger, sellTrigger: Trigger, prices: Map[String, PriceDataPoint]): Boolean = {
    isTriggerActive(symbol,buyTrigger,prices) && !isTriggerActive(symbol,sellTrigger,prices)
  }

  def isTriggerActive(symbol: String, trigger: Trigger, prices: Map[String, PriceDataPoint]): Boolean = {
    trigger match {
      case ConstantTrigger(active) => active
      case PriceTrigger(above, threshHold) => prices.get(symbol).exists(pdp => (pdp.price > threshHold) == above)
    }
  }
}

object RandomUtils {
  def valueOfHoldings(holdings: List[Holding], prices: Map[String,PriceDataPoint]): Double = {
    holdings.foldLeft(0.0)((nw, h) => nw + (h.amount * prices.get(h.symbol).map(pdp => pdp.price).getOrElse(0.0)))
  }
}