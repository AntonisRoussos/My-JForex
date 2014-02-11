package macd;

import com.dukascopy.api.*;
import java.util.HashSet;
import java.util.Set;
import com.dukascopy.api.util.DateUtils;

// Add dynamic TP, SL

public class MACDRSIv6 implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IBar previousBar;
    private IOrder order;
    private IOrder previousOrder;
    private boolean orderIsChanged = true;
    private double latestPrice = 0;
    private int n = 1;
    private double stopLossPrice;
    private double takeProfitPrice;
    
    @Configurable(value = "Instrument value")
    public Instrument myInstrument = Instrument.EURGBP;
    @Configurable(value = "Offer Side value", obligatory = true)
    public OfferSide myOfferSide;
    @Configurable(value = "Period value")
    public Period myPeriod = Period.FOUR_HOURS;
    @Configurable("MACD fast time period")
    public int fastTimePeriod = 12;
    @Configurable("MACD slow time period")
    public int slowTimePeriod = 26;
    @Configurable("MACD signal time period")
    public int signalTimePeriod = 9;
    @Configurable("RSI time period")
    public int timePeriod = 14;
    @Configurable("RSI up")
    public int rsiUp = 67;
    @Configurable("RSI down")
    public int rsiDown = 31;
    @Configurable("TP-SL increament in pips")
    public int increamentTPSLPips = 10;
    @Configurable("percentage increament")
    public double percentageIncreament = 0.8;
    @Configurable("Filter")
    public Filter filter = Filter.WEEKENDS;
  

    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
    this.userInterface = context.getUserInterface();

        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(myInstrument);
        context.setSubscribedInstruments(instruments, true);
    }//end of onStart method

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
        if (message.getOrder() != null) {
            printMe("order: " + message.getOrder().getLabel() + " || message content: " + message);
        }
    }

  public void onStop() throws JFException {}


  public void onTick(Instrument instrument, ITick tick) throws JFException {

    if (!instrument.equals(myInstrument)) {
    return; //quit
    }

    double lastTickBid = 0;
    double lastTickAsk = 0;
    double stopLossValue = 0;
    double stopLossPrice = 0;
    double takeProfitPrice = 0;
    double takeProfitthreshold = 0;
    double stopLossValueForLong =0;

    previousOrder = engine.getOrder("MyStrategyOrder");     

	lastTickBid = history.getLastTick(myInstrument).getBid();
	lastTickAsk = history.getLastTick(myInstrument).getAsk();

    if (previousOrder != null) {
        takeProfitthreshold = previousOrder.isLong() ? (takeProfitPrice - percentageIncreament * increamentTPSLPips) : (takeProfitPrice + percentageIncreament * increamentTPSLPips);
	}

    if (previousOrder != null
        && ((lastTickAsk >= takeProfitthreshold && previousOrder.getOrderCommand() == IEngine.OrderCommand.SELL)
        || (lastTickBid <= takeProfitthreshold && previousOrder.getOrderCommand() == IEngine.OrderCommand.BUY))) {
        stopLossValue = myInstrument.getPipValue() * increamentTPSLPips * n;
        stopLossPrice = previousOrder.isLong() ? (stopLossPrice - stopLossValue) : (stopLossPrice + stopLossValue);
        takeProfitPrice = previousOrder.isLong() ? (takeProfitPrice + stopLossValue) : (takeProfitPrice - stopLossValue);
        previousOrder.setStopLossPrice(stopLossPrice);
        previousOrder.setTakeProfitPrice(takeProfitPrice);
	long lastTickTime = history.getLastTick(myInstrument).getTime();
        console.getOut().format(DateUtils.format(lastTickTime)).println();        
        printMe(String.format("New TP = %.5f; New SL = %.5f", takeProfitPrice, stopLossPrice));
    }
      
  }//end of onTick method

    
    /*
    * Check if all previous macd histogram values have the same sign and the current (latest)
    * have opposite sign to trigger th execution of an order
    */
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    if (!instrument.equals(myInstrument) || !period.equals(myPeriod)) {
        return; //quit
    }

    int shift = 1;
    IBar currentBar = history.getBar(instrument, period, myOfferSide, shift);
    long currentBarTimeL = currentBar.getTime();
    


    int candlesBefore = 4, candlesAfter = 0;
//    long completedBarTimeL = myOfferSide == OfferSide.ASK ? askBar.getTime() : bidBar.getTime();
    double macd [][] = indicators.macd(instrument, period, myOfferSide, IIndicators.AppliedPrice.CLOSE,
            fastTimePeriod, slowTimePeriod, signalTimePeriod, filter, candlesBefore, currentBarTimeL, candlesAfter);
    double rsi[] = indicators.rsi(instrument, period, myOfferSide, IIndicators.AppliedPrice.CLOSE,
            timePeriod, filter, candlesBefore, currentBarTimeL, candlesAfter);

    IEngine.OrderCommand myCommand = null;
    previousOrder = engine.getOrder("MyStrategyOrder");            

//    if (macd[2][3] > 0 && macd[2][2] < 0 && macd[2][1] < 0 && macd[2][0] < 0) {
    if ((macd[2][2] > macd[2][1] && rsi[shift] < rsiDown) 
        && (previousOrder == null || (previousOrder != null && previousOrder.getOrderCommand() == IEngine.OrderCommand.SELL))){
        printMe("MACD Histogram goes UP"); //indicator goes up
        myCommand = IEngine.OrderCommand.BUY;
//    } else if (macd[2][3] < 0 && macd[2][2] > 0 && macd[2][1] > 0 && macd[2][0] > 0) {
    } else if ((macd[2][2] < macd[2][1] && rsi[shift] > rsiUp) 
        && (previousOrder == null || (previousOrder != null && previousOrder.getOrderCommand() == IEngine.OrderCommand.BUY))){
        printMe("MACD Histogram goes DOWN"); //indicator goes down
        myCommand = IEngine.OrderCommand.SELL;
    } else if (rsi[2] > rsi[1] && rsi[2] > rsi[3] && myCommand == IEngine.OrderCommand.BUY) {
    } else if (rsi[2] < rsi[1] && rsi[2] < rsi[3] && myCommand == IEngine.OrderCommand.SELL) {
    } else {
        return;
    }

    //if macd trend direction is changed, then create a new order
    orderIsChanged = false;
    order = engine.getOrder("MyStrategyOrder");                      
    if(order != null && engine.getOrders().contains(order)){
        order.close();
        order.waitForUpdate(IOrder.State.CLOSED); //wait till the order is closed
        console.getOut().println("Order " + order.getLabel() + " is closed");
    } else if (order == null) {
        console.getOut().println("No order to close");
    }              
                
    double lastTickBid = history.getLastTick(myInstrument).getBid();
    double lastTickAsk = history.getLastTick(myInstrument).getAsk();
    double stopLossValueForLong = myInstrument.getPipValue() * increamentTPSLPips;
    double stopLossValueForShort = myInstrument.getPipValue() * increamentTPSLPips;
    stopLossPrice = myCommand.isLong() ? (lastTickBid - stopLossValueForLong) : (lastTickAsk + stopLossValueForLong);
    takeProfitPrice = myCommand.isLong() ? (lastTickBid + stopLossValueForShort) : (lastTickAsk - stopLossValueForShort);

    if(myCommand != null){
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe(String.format("Bar MACD Values Current : MACD = %.5f; MACD Signal = %.5f, ; MACD Hist = %.5f", macd[0][1], macd[1][1], macd[2][1]));
        printMe(String.format("RSI = %.5f", rsi[1]));
    
        previousBar = myOfferSide == OfferSide.ASK ? askBar : bidBar;
        console.getOut().println(" || PreviousBar- --> " + previousBar + " || Period- --> " + period + " || Instrument- --> " + instrument);              
        engine.submitOrder("MyStrategyOrder", myInstrument, myCommand, 0.1, 0, 1, stopLossPrice, takeProfitPrice);   
        orderIsChanged = false;
    }// myCommand != null
    }//end of onBar method

    private void printMe(Object toPrint) {
        console.getOut().println(toPrint);
    }
    

    private void printMeError(Object o) {
        console.getErr().println(o);
    }
    
}
