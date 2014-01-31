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
    @Configurable("periods back")
    public int periodsBack = 48;
    @Configurable("SAR maximum")
    public double sarMaximum = 0.2;
    @Configurable("RSI time period")
    public int timePeriod = 14;
    @Configurable("RSI Up Low")
    public int RSIUpLow = 50;
    @Configurable("RSI UP High")
    public int RSIUpHigh = 75;
    @Configurable("RSI Down Low")
    public int RSIDownLow = 25;
    @Configurable("RSI Down High")
    public int RSIDownHigh = 50;
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
    IBar periodsBackBar = history.getBar(instrument, period, myOfferSide, periodsBack);
    long currentBarTimeL = currentBar.getTime();

    double rsi[] = indicators.rsi(instrument, period, myOfferSide, IIndicators.AppliedPrice.CLOSE,
            timePeriod, shift);

    IEngine.OrderCommand myCommand = null;
    previousOrder = engine.getOrder("MyStrategyOrder");            

//  Sharp down move
    if (rsi[shift] > RSIDownHigh  && previousOrder != null && periodsBackBar.getClose()-currentBar.getClose()> 0.003) {
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe("Sharp Down"); 
        printMe("RSI High"); 
        myCommand = IEngine.OrderCommand.SELL;
    } else if (rsi[shift] < RSIDownLow  && previousOrder != null && periodsBackBar.getClose()-currentBar.getClose()> 0.003) {
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe("Sharp Down"); 
        printMe("RSI Low"); 
        myCommand = IEngine.OrderCommand.BUY;
    } else if (rsi[shift] < RSIUPLow  && previousOrder != null && currentBar.getClose()-periodsBackBar.getClose()> 0.003){
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe("Sharp Up"); 
        printMe("RSI Low"); 
        myCommand = IEngine.OrderCommand.SELL;
    } else if (rsi[shift] > RSIUPHigh  && previousOrder != null && currentBar.getClose()-periodsBackBar.getClose()> 0.003){
        console.getOut().format(DateUtils.format(currentBarTimeL)).println();        
        printMe("Sharp Up"); 
        printMe("RSI High"); 
        myCommand = IEngine.OrderCommand.SELL;
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
    } else if (order == null) {
        console.getOut().println("No order to close");
    }              
                
    double lastTickBid = history.getLastTick(myInstrument).getBid();
    double lastTickAsk = history.getLastTick(myInstrument).getAsk();
    double stopLossValueForLong = myInstrument.getPipValue() * stopLossPips;
    double stopLossValueForShort = myInstrument.getPipValue() * takeProfitPips;

    if(myCommand != null){
  
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
