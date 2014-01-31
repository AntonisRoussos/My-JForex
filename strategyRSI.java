package JForex;

import com.dukascopy.api.*;
import java.util.HashSet;
import java.util.Set;
import com.dukascopy.api.util.DateUtils;


public class RSISARv3 implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IBar previousBar;
    private IOrder order;
    private IOrder previousOrder;
    private String previousSignal = "N";
    private String currentSignal = "N";
    private boolean buyFlag = false;
    private boolean sellFlag = false;
    private int closeCounter = 0;
    
    @Configurable(value = "Instrument value")
    public Instrument myInstrument = Instrument.EURUSD;
    @Configurable(value = "Offer Side value", obligatory = true)
    public OfferSide myOfferSide;
    @Configurable(value = "Period value")
    public Period myPeriod = Period.FIVE_MINS;
    @Configurable("SAR acceleration")
    public double sarAcceleration = 0.02;
    @Configurable("SAR maximum")
    public double sarMaximum = 0.2;
    @Configurable("RSI time period")
    public int timePeriod = 14;
    @Configurable("RSI Max Limit")
    public double RSIMaximum = 70;
    @Configurable("RSI Min Limit")
    public double RSIMinimum = 30;
    @Configurable("Stop loss in pips")
    public int stopLossPips = 10;
    @Configurable("Take profit in pips")
    public int takeProfitPips = 10;
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
    IBar previousBar = history.getBar(instrument, period, myOfferSide, 2);

    int candlesBefore = 4, candlesAfter = 0;
//    long completedBarTimeL = myOfferSide == OfferSide.ASK ? askBar.getTime() : bidBar.getTime();
//    double sar[] = indicators.sar(instrument, period, myOfferSide, sarAcceleration, sarMaximum, Filter.NO_FILTER, candlesBefore, currentBarTimeL, candlesAfter);
    double rsi[] = indicators.rsi(instrument, period, myOfferSide, IIndicators.AppliedPrice.CLOSE,
            timePeriod, Filter.NO_FILTER, candlesBefore, currentBarTimeL, candlesAfter);

    IEngine.OrderCommand myCommand = null;
      
     double sarc = indicators.sar(instrument, period, myOfferSide, sarAcceleration, sarMaximum, 2);
     double sarp = indicators.sar(instrument, period, myOfferSide, sarAcceleration, sarMaximum, 3);
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe(String.format("SAR current = %.5f; Current Close = %.5f", sarc, currentBar.getClose()));
        printMe(String.format("SAR Previous = %.5f; Previous Close = %.5f", sarp, previousBar.getClose()));
//     if ((sar[3] >= currentBar.getClose() && sar[2] <= previousBar.getClose()) || (sar[3] <= currentBar.getClose() && sar[2] >= previousBar.getClose())){
     if ((sarc >= currentBar.getClose() && sarp <= previousBar.getClose()) || (sarc <= currentBar.getClose() && sarp >= previousBar.getClose())){
        closeCounter++;
        printMe(closeCounter);
    }

          
    previousOrder = engine.getOrder("MyStrategyOrder");            
              
//    if ((rsi[1] <= RSIMinimum || rsi[2] <= RSIMinimum || rsi[3] <= RSIMinimum) && currentBar.getOpen() < currentBar.getClose() && (previousOrder == null || (previousOrder != null && previousOrder.getOrderCommand() == IEngine.OrderCommand.SELL))) {
//    if ((rsi[3] <= RSIMinimum) && currentBar.getOpen() < currentBar.getClose() && (previousOrder == null || (previousOrder != null && previousOrder.getOrderCommand() == IEngine.OrderCommand.SELL))) {
    if ((rsi[3] <= RSIMinimum) && currentBar.getOpen() < currentBar.getClose() && previousBar.getOpen() < previousBar.getClose() && (previousOrder == null || (previousOrder != null && previousOrder.getOrderCommand() == IEngine.OrderCommand.SELL))) {
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe("RSI Down"); 
        printMe(String.format("Current Open = %.5f; Current Close = %.5f", currentBar.getOpen(), currentBar.getClose()));
        printMe(String.format("RSI = %.5f", rsi[shift]));
        myCommand = IEngine.OrderCommand.BUY;
        buyFlag = true;
        if (sarc >= currentBar.getClose()){
            closeCounter = 1;
        } else {
            closeCounter = 2;
        }
    } else if ((rsi[3] >= RSIMaximum) && currentBar.getOpen() >= currentBar.getClose() && previousBar.getOpen() >= previousBar.getClose() && (previousOrder == null || (previousOrder != null && previousOrder.getOrderCommand() == IEngine.OrderCommand.BUY))) {
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe("RSI Up"); 
        printMe(String.format("Current Open = %.5f; Current Close = %.5f", currentBar.getOpen(), currentBar.getClose()));
        printMe(String.format("RSI = %.5f", rsi[3]));
        myCommand = IEngine.OrderCommand.SELL;
        sellFlag = true;
        if (sarc >= currentBar.getClose()){
            closeCounter = 2;
        } else {
            closeCounter = 1;
        }
    } else if (closeCounter == 3){
    } else {
        return;
    }

    

    order = engine.getOrder("MyStrategyOrder");                      
    if(order != null && engine.getOrders().contains(order)){
        order.close();
        order.waitForUpdate(IOrder.State.CLOSED); //wait till the order is closed
        console.getOut().println("Order " + order.getLabel() + " is closed");
        previousSignal = "N";
        currentSignal = "N";    
//        buyFlag = false;
//        sellFlag = false;
    } else if (order == null) {
        console.getOut().println("No order to close");
    }              
                
    double lastTickBid = history.getLastTick(myInstrument).getBid();
    double lastTickAsk = history.getLastTick(myInstrument).getAsk();
    double stopLossValueForLong = myInstrument.getPipValue() * stopLossPips;
    double stopLossValueForShort = myInstrument.getPipValue() * takeProfitPips;

    if(myCommand != null){
//        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
//        printMe(String.format("Bar MACD Values Current : MACD = %.5f; MACD Signal = %.5f, ; MACD Hist = %.5f", macd[0][3], macd[1][3], macd[2][3]));
//        printMe(String.format("RSI = %.5f", rsi[shift]));
    
        double stopLossPrice = myCommand.isLong() ? (lastTickBid - stopLossValueForLong) : (lastTickAsk + stopLossValueForLong);
        double takeProfitPrice = myCommand.isLong() ? (lastTickBid + stopLossValueForShort) : (lastTickAsk - stopLossValueForShort);
        previousBar = myOfferSide == OfferSide.ASK ? askBar : bidBar;
//        console.getOut().println(" || PreviousBar- --> " + previousBar + " || Period- --> " + period + " || Instrument- --> " + instrument);              
        engine.submitOrder("MyStrategyOrder", myInstrument, myCommand, 0.1, 0, 1, stopLossPrice, takeProfitPrice);          
    }// myCommand != null
    }//end of onBar method

    private void printMe(Object toPrint) {
        console.getOut().println(toPrint);
    }
    

    private void printMeError(Object o) {
        console.getErr().println(o);
    }
    
}
