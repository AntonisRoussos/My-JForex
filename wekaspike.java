package jforex;
 
import java.util.*;
import java.io.*;
import java.text.*;
import java.math.*;
import java.lang.Math;
import com.dukascopy.api.*;

@RequiresFullAccess
public class DataFeederv6 implements IStrategy {
   private IEngine engine;
   private IConsole console;
   private IHistory history;
   private IContext context;
   private IIndicators indicators;
   private IUserInterface userInterface;
     
    @Configurable("Instrument")
    public Instrument instrument = Instrument.EURUSD;
 
    @Configurable("Period")
    public Period period = Period.FIVE_MINUTES;
     
    @Configurable("OfferSide")
    public OfferSide offerSide = OfferSide.BID	;

    @Configurable("Periods Back")
    public int periodsBack = 4;

    @Configurable("Volume Multiplier")
    public int volumeMultiplier = 4;

    @Configurable("pips fot the Spike")
    public int pipsSpike = 30;
    
    @Configurable("Periods Forward prediction")
    public int periodsForward = 12;
    
    
 

    @Configurable("File")
    public File file;
     
    private Writer out;
    private DateFormat dateFormat;
    private DecimalFormat priceFormat;
 
    public void onStart(IContext context) throws JFException {
      this.engine = context.getEngine();
      this.console = context.getConsole();
      this.history = context.getHistory();
      this.context = context;
      this.indicators = context.getIndicators();
      this.userInterface = context.getUserInterface();
         
        dateFormat = new SimpleDateFormat("yyyy.MM.dd,HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
         
        priceFormat = new DecimalFormat("0.#####");
 
        if (file == null || file.getPath().equals("")) {
            console.getErr().println("File not selected");
            context.stop();
            return;
        }
        try {
            out = new BufferedWriter(new FileWriter(file));
        } catch (Exception e) {
            console.getErr().println(e.getMessage());
            e.printStackTrace(console.getErr());
            context.stop();
        }
        
     try {
        out.write("volume" + "," + "spike");
    } catch (Exception e) {
        console.getErr().println(e.getMessage());
        e.printStackTrace(console.getErr());
        context.stop();
    }
       
    for (int i = 1; i < daysBack + 1; i++) {

            IBar previousBar = history.getBar(instrument, period, offerSide, i);
            try {
                out.write("," + "pips" + i);
            } catch (Exception e) {
                console.getErr().println(e.getMessage());
                e.printStackTrace(console.getErr());
                context.stop();
            }
            
    }

    
    try {
    out.write(",bssignal\r\n");
    } catch (Exception e) {
        console.getErr().println(e.getMessage());
        e.printStackTrace(console.getErr());
        context.stop();
    }
    
    }
 
   public void onAccount(IAccount account) throws JFException {
   }
 
   public void onMessage(IMessage message) throws JFException {
   }
 
   public void onStop() throws JFException {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                console.getErr().println(e.getMessage());
                e.printStackTrace(console.getErr());
                context.stop();
            }
        }
   }
 
   public void onTick(Instrument instrument, ITick tick) throws JFException {
   }
   
   public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (instrument != this.instrument || period != this.period) {
            return;
        }
        
    	IBar spikeBar = history.getBar(instrument, period, offerSide, periodsForward);

	dounle Volume = 0.0;
	
   	for (int i = periodsForward; i < periodsForward + daysBack + 1; i++) {
		IBar previousBar = history.getBar(instrument, period, offerSide, i);
		volume = volume + previousBar.getVolume();
        }
        double averageVolume = volume / (daysBack + 1);
        if (spikeBar.getVolume() < averageVolume * volumeMultiplier ||
        	abs(spikeBar.getClose()-spikeBar.getOpen())> 0.1 * abs(spikeBar.getHigh()-spikeBar.getLow()) {
            return;
        }
   
	try {
		 out.write(priceFormat.format(spikeBar.getVolume() / averageVolume) + ","
		 		+ priceFormat.format(abs(spikeBar.getHigh()-spikeBar.getLow()) - abs(spikeBar.getHigh()-spikeBar.getLow());
	    } catch (Exception e) {
	        console.getErr().println(e.getMessage());
	        e.printStackTrace(console.getErr());
	        context.stop();
	    }


    for (int i = periodsForward; i < periodsForward + daysBack + 1; i++) {
	IBar previousBar = history.getBar(instrument, period, offerSide, i);
   
	    try {
	        out.write("," + priceFormat.format(previousBar.getClose()-spikeBar.getClose()));
	    } catch (Exception e) {
	        console.getErr().println(e.getMessage());
	        e.printStackTrace(console.getErr());
	        context.stop();
	    }
            
	}

	String bssignal = new String();
        IBar previousBar = history.getBar(instrument, period, offerSide, 1);

        if (spikeBar.getClose() <= previousBar.getClose()) {
            bssignal = "S";
        } 		
        else {
            bssignal = "B";
        };

//        printMe(String.format("Current Close = %.5f; Previous Close = %.5f, ; return1 = %.5f", previousBar.getClose(), lastBar.getClose(), return1));
        
        try {
        out.write("," + String.format(bssignal) + "\r\n");
        } catch (Exception e) {
            console.getErr().println(e.getMessage());
            e.printStackTrace(console.getErr());
            context.stop();
        }

   }
    private void printMe(Object toPrint) {
        console.getOut().println(toPrint);
    }

    private void printMeError(Object o) {
        console.getErr().println(o);
    }
}
