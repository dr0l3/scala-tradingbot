package droletours

class Ticker {

  def calculateHoldings(userState: UserState, prices: Map[String, PriceDataPoint]): UserState = {
    val netWorth = userState.strategies
      .foldLeft(userState.capital)((nw, mPdp) =>  nw + RandomUtils.valueOfHoldings(mPdp._1, prices))

    val fundsPerStrategy = netWorth / userState.strategies.size

    val (activeStrategies, inactiveStrategies) =
      userState.strategies.partition { case (_, userStrategy) =>
        TriggerUtils.activeSymbols(userStrategy.selector, userStrategy.buyTrigger, userStrategy.sellTrigger, prices).nonEmpty
      }

    val newStrategies = activeStrategies
      .map { case (_, userStrategy) =>
        (allocatePerSymbol(SymbolResolver.resolveSymbols(userStrategy.selector), fundsPerStrategy, prices), userStrategy)
      }

    val valueOfNewStrategies = RandomUtils.valueOfHoldings(newStrategies.flatMap { case (holdings, _) => holdings }, prices)
    val markup = netWorth / valueOfNewStrategies
    val strategiesAfterMarkup = newStrategies
      .map { case (holdings, userStrategy) =>
        (holdings.map(holding => holding.copy(amount = holding.amount * markup)), userStrategy)
      } ++ inactiveStrategies
      .map { case (_, userStrategy) => (Nil, userStrategy) }
    val valueAfterMarkup = RandomUtils.valueOfHoldings(strategiesAfterMarkup.flatMap(p => p._1), prices)
    val nextCapital = netWorth - valueAfterMarkup

    userState.copy(strategies = strategiesAfterMarkup, capital = nextCapital, netWorth = netWorth)
  }

  def allocatePerSymbol(symbols: List[String], fundsToAllocate: Double, prices: Map[String, PriceDataPoint]): List[Holding] = {
    val activeSymbols = symbols
      .map(symbol => prices.get(symbol))
      .foldLeft(List.empty[PriceDataPoint])((acc, opt) => opt.map(_ :: acc).getOrElse(acc))
    val fundsPerSymbol = fundsToAllocate / activeSymbols.size
    activeSymbols.map(pdp => Holding(pdp.symbol, fundsPerSymbol / pdp.price))
  }

  def collapse[T](list: List[Option[T]]): List[T] = {
    list.foldLeft(List.empty[T])((acc, next) => next.map(_ :: acc).getOrElse(acc))
  }
}