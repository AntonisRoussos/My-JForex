import datetime
import matplotlib.pyplot as plt
import matplotlib.finance as finance
import pandas as pd
import numpy as np
import math
import csv
import time
import matplotlib.mlab as mlab
import pandas.stats.moments as mom
import csv
import sqlite3

#db = sqlite3.connect("results.sqlite")
db = sqlite3.connect("resultsNASDAQ.sqlite")

def mean(array):
    ''' average '''
    return np.mean(array, axis = 0)

def stddev(array):
    ''' Standard Deviation '''
    return np.std(array, axis = 0)

def init_db(cur):
    cur.execute('DROP TABLE IF EXISTS stockPerformances ')
    cur.execute('CREATE TABLE stockPerformances ('
        'Ticker TEXT, '
        'PL REAL, '
        'Trades INTEGER, '
        'EMA REAL, '
        'Price REAL, '
        'AverageVolume REAL, '
        'Today TEXT, '
        'VolumeIndicator REAL, '
        'StandardDev REAL )')

def clear_db(cur):
    cur.execute('DELETE FROM stockPerformances')

def populate_db(cur, results):
    cur.executemany("""INSERT INTO stockPerformances (Ticker, PL, Trades, EMA, Price, AverageVolume, Today, VolumeIndicator, StandardDev) VALUES (?,?,?,?,?,?,?,?, ?)""", results)

def ticker_data(ticker):
    print ticker
    startdate = datetime.date(2012,10,28)
    today = enddate = datetime.date.today()
    pricesd = np.zeros(1000)
    datesd = np.zeros(1000)
    volumed = np.zeros(1000)

    try:
        fh = finance.fetch_historical_yahoo(ticker, startdate, enddate)
        # a numpy record array with fields: date, open, high, low, close, volume, adj_close)
        r = mlab.csv2rec(fh); fh.close()
        r.sort()
        prices = r.adj_close
        dates = r.date
        volume = r.volume
    except:
        prices = pricesd
        dates = datesd
        volume = volumed
        print "Unexpected error:"
#        raise
        
    return prices, dates, np.mean(volume), volume


def derivative(y_data):
# calculates the 1st derivative'''
  y = (y_data[1:]-y_data[:-1])
  dy = y/2
  dy1 = np.zeros(len(dy)+1,float)
  dy1[0] = dy[0]
  for i in drange(1,len(dy1),1):
    dy1[i] = dy[i-1]
 #  dy = y
  #scaling factor that is not necessary but useful for my application
  #one more value is added because the length
  # of y and dy are 1 less than y_data
  Derivative = np.array(dy1)
#  print Derivative
#  print len(Derivative)
  return Derivative

def moving_average_convergence(x, nslow=50, nfast=10):
    """
    compute the crossover (Moving Average Convergence/Divergence) using a fast and slow exponential moving avg'
    return value is emaslow, emafast, crossover which are len(x) arrays
    """
    emaslow = mom.ewma(x, span=nslow)
    emafast = mom.ewma(x, span=nfast)
#    emaslow = moving_average(x, nslow, type='exponential')
#    emafast = moving_average(x, nfast, type='exponential')
#    print x
#    time.sleep(10)
    return emaslow, emafast, emafast - emaslow

def crossoversignal(ticker, prices, dates, volumes):
### compute the crossover indicator
    commision = 0.005
    m_commision = commision + 1
    d_commision = 1 - commision
    tax = 0.998
    capital = 1000000.0
    capital_derivative = 1000000.0
    nslow = 50
    nfast = 5
    trxsignal=np.empty(252,dtype='string')
    emaslow, emafast, crossover = moving_average_convergence(prices, nslow=nslow, nfast=nfast)
    emafastderivative = derivative(emafast)
#    print prices
#    print crossover
    last_status='H'
    last_status_derivative='H'

    number_of_trades_derivatives = 0
    last_trx_price_derivatives = 0
    today = 'N'
    yesterday = datetime.date.today() - datetime.timedelta(days=1)
    for index in drange(1,len(dates),1):
 #       print emafastderivative
        if emafastderivative[index-1] < 0 and emafastderivative[index] >= 0 and (last_status_derivative=='S' or last_status_derivative=='H'):
            number_of_trades_derivatives = number_of_trades_derivatives + 1
            last_status_derivative='B'
            capital_derivative = capital_derivative - prices[index] * m_commision
            last_trx_price_derivatives = prices[index]
#            print dates[index]
#            print yesterday
            if dates[index] == yesterday:
                today = 'Y'
        if emafastderivative[index-1] > 0 and emafastderivative[index] <= 0 and (last_status_derivative=='B'):
            last_status_derivative='S'
            capital_derivative = capital_derivative + prices[index] * d_commision * tax
            number_of_trades_derivatives = number_of_trades_derivatives + 1
    if last_status_derivative == 'B':
            capital_derivative = capital_derivative + last_trx_price_derivatives
    pl_derivatives = (capital_derivative - 1000000.0)*100/ np.mean(prices, axis = 0)
    volume_indicator = mean(volumes[len(volumes)-2:len(volumes)-1]) / mean(volumes[0:len(volumes)])
    print ticker
    print pl_derivatives
    print number_of_trades_derivatives
    print 
#    time.sleep(6)
            
#    plt.plot(crossover)
#    plt.ylabel('crossover')
#    plt.show()
#    print index
#    print len(dates)
#    print len(emafast)
#    print len(prices)
#    print len(emafastderivative)
#    print emafast[len(emafastderivative)]
    return ticker, pl_derivatives, number_of_trades_derivatives, emafast[index], today , prices[index], volume_indicator


def drange(start, stop, step):
    r = start
    while r < stop:
      	yield r
      	r += step

def read_all_stocks():
    previous_symbol = ''
    tickers=[]
    results=[]
    results1=[]
    results2=[]
    results3=[]
    ticker_results=[]
#    myfile = open('sp500.txt',"r")
    myfile = open('nasdaq.csv',"r")

    # Read in the data
    counter = 0  
    csv_reader = csv.reader(myfile)
    for row in csv_reader: 
        counter = counter + 1
        if row[0] != 'Symbol':
          ticker=row[0]
          prices, dates, average_volume, volume = ticker_data(ticker)
          ticker, pl_derivatives, number_of_trades_derivatives, ema, today, price, volume_indicator = crossoversignal(ticker, prices, dates, volume)
          del results1[:]
          results1.append(ticker)
          results1.append(pl_derivatives)
          results1.append(number_of_trades_derivatives)
          results1.append(ema)
          results1.append(price)
          results1.append(average_volume)
          results1.append(today)
          results1.append(volume_indicator)
          results1.append(stddev(prices))
          results2 = tuple(results1)
          results3.append(results2)
#    atickers = tuple(atickers)
#    apl_derivatives = tuple(apl_derivatives)
#    anumber_of_trades_derivatives = tuple(anumber_of_trades_derivatives)
#    aema = tuple(aema)
#    aaverage_volume = tuple(aaverage_volume)
#    atoday = tuple(atoday)
#    results.append(atickers)
#    results.append(apl_derivatives)
#    results.append(anumber_of_trades_derivatives)
#    results.append(aema)
#    results.append(aaverage_volume)
#    results.append(atoday)
    
    cur = db.cursor()
    init_db(cur)
    clear_db(cur)
    populate_db(cur, results3)
    db.commit()    
#    results = np.array(results, dtype='|S10')
#    results = results.reshape(len(results)/6, 6)
#    fl = open('results_all_nasdaq.csv','w')
#    writer = csv.writer(fl)
#    writer.writerow(['ticker', 'profit%', 'number of trades'])
#    for values in results:
#        writer.writerow(values)
#    fl.close()    
    # Read in the data
 #               print closing_prices
 #               print stock_date
  #              print crossovermatrix

read_all_stocks()
