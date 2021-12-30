package com.romzkie.ultrasshservice;


import android.net.TrafficStats;
import android.os.SystemClock;
import java.util.ArrayList;

public class StatisticGraphData 
{

	private static StatisticGraphData statisticData;
	private boolean m_displayDataTransferStats;
	private StatisticGraphData.DataTransferStats m_dataTransferStats;

	private StatisticGraphData()
	{
		m_dataTransferStats = new DataTransferStats();
        m_displayDataTransferStats = false;

	}
	public static synchronized StatisticGraphData getStatisticData()
    {
        if (statisticData == null)
        {
            statisticData = new StatisticGraphData();
        }

        return statisticData;
    }
	public synchronized void setDisplayDataTransferStats(boolean displayDataTransferStats)
    {
        m_displayDataTransferStats = displayDataTransferStats;
    }

    public synchronized boolean getDisplayDataTransferStats()
    {
        return m_displayDataTransferStats;
    }

    public synchronized DataTransferStats getDataTransferStats()
    {
        return m_dataTransferStats;
    }

    public class DataTransferStats
    {
        private boolean m_isConnected;
        private long m_connectedTime;

        private long m_totalBytesSent;
        private long m_totalBytesReceived;

        public final static long SLOW_BUCKET_PERIOD_MILLISECONDS = 5*60*1000; 
        public final static long FAST_BUCKET_PERIOD_MILLISECONDS = 1000;
        public final static int MAX_BUCKETS = 24 * 60 / 5;

		private long mLastSent;

		private long mSent;

		private long mLastReceived;

		private long mReceived;

        private class Bucket
        {
            public long m_bytesSent = 0;
            public long m_bytesReceived = 0;
        }

        private ArrayList<Bucket> m_slowBuckets;
        private long m_slowBucketsLastStartTime;
        private ArrayList<Bucket> m_fastBuckets;
        private long m_fastBucketsLastStartTime;

        DataTransferStats()
        {
            m_totalBytesSent = 0;
            m_totalBytesReceived = 0;

            stop();
        }

        public synchronized void startSession()
        {

            resetBytesTransferred();

        }

        public synchronized void startConnected()
        {
            this.m_isConnected = true;
            this.m_connectedTime = SystemClock.elapsedRealtime();

        }

        public synchronized void stop()
        {

			this.m_totalBytesReceived = 0;
			this.m_totalBytesSent = 0;
            this.m_isConnected = false;
            this.m_connectedTime = 0;
            resetBytesTransferred();
        }

        private void resetBytesTransferred()
        {
            long now = SystemClock.elapsedRealtime();
            this.m_slowBucketsLastStartTime = bucketStartTime(now, SLOW_BUCKET_PERIOD_MILLISECONDS);
            this.m_slowBuckets = newBuckets();
            this.m_fastBucketsLastStartTime = bucketStartTime(now, FAST_BUCKET_PERIOD_MILLISECONDS);
            this.m_fastBuckets = newBuckets();
        }

        public synchronized void addBytesSent(long bytes)
        {
            this.m_totalBytesSent += bytes;

            manageBuckets();
            addSentToBuckets(bytes);
        }

        public synchronized void addBytesReceived(long bytes)
        {
            this.m_totalBytesReceived += bytes;

            manageBuckets();
            addReceivedToBuckets(bytes);
        }

        private long bucketStartTime(long now, long period)
        {
            return period*(now/period);
        }

        private ArrayList<Bucket> newBuckets()
        {
            ArrayList<Bucket> buckets = new ArrayList<Bucket>();
            for (int i = 0; i < MAX_BUCKETS; i++)
            {
                buckets.add(new Bucket());
            }
            return buckets;
        }

        private void shiftBuckets(long diff, long period, ArrayList<Bucket> buckets)
        {
            for (int i = 0; i < diff/period + 1; i++)
            {
                buckets.add(buckets.size(), new Bucket());
                if (buckets.size() >= MAX_BUCKETS)
                {
                    buckets.remove(0);
                }
            }            
        }

        private void manageBuckets()
        {
            long now = SystemClock.elapsedRealtime();

            long diff = now - this.m_slowBucketsLastStartTime;            
            if (diff >= SLOW_BUCKET_PERIOD_MILLISECONDS)
            {
                shiftBuckets(diff, SLOW_BUCKET_PERIOD_MILLISECONDS, m_slowBuckets);
                this.m_slowBucketsLastStartTime = bucketStartTime(now, SLOW_BUCKET_PERIOD_MILLISECONDS);
            }

            diff = now - this.m_fastBucketsLastStartTime;            
            if (diff >= FAST_BUCKET_PERIOD_MILLISECONDS)
            {
                shiftBuckets(diff, FAST_BUCKET_PERIOD_MILLISECONDS, m_fastBuckets);
                this.m_fastBucketsLastStartTime = bucketStartTime(now, FAST_BUCKET_PERIOD_MILLISECONDS);
            }
        }

        private ArrayList<Long> getSentSeries(ArrayList<Bucket> buckets)
        {
            ArrayList<Long> series = new ArrayList<Long>();
            for (int i = 0; i < buckets.size(); i++)
            {
                series.add(buckets.get(i).m_bytesSent);
            }
            return series;
        }

        private ArrayList<Long> getReceivedSeries(ArrayList<Bucket> buckets)
        {
            ArrayList<Long> series = new ArrayList<Long>();
            for (int i = 0; i < buckets.size(); i++)
            {
                series.add(buckets.get(i).m_bytesReceived);
            }
            return series;
        }

        private void addSentToBuckets(long bytes)
        {
            this.m_slowBuckets.get(this.m_slowBuckets.size()-1).m_bytesSent += bytes;
            this.m_fastBuckets.get(this.m_fastBuckets.size()-1).m_bytesSent += bytes;
        }

        private void addReceivedToBuckets(long bytes)
        {
            this.m_slowBuckets.get(this.m_slowBuckets.size()-1).m_bytesReceived += bytes;
            this.m_fastBuckets.get(this.m_fastBuckets.size()-1).m_bytesReceived += bytes;
        }

        public synchronized boolean isConnected()
        {
            return this.m_isConnected;
        }

        public synchronized long getElapsedTime()
        {
            long now = SystemClock.elapsedRealtime();

            return now - this.m_connectedTime;
        }

        public synchronized long getTotalBytesSent()
        {
            return this.m_totalBytesSent;
        }

        public synchronized long getTotalBytesReceived()
        {
            return this.m_totalBytesReceived;
        }

        public synchronized ArrayList<Long> getSlowSentSeries()
        {
            manageBuckets();
            return getSentSeries(this.m_slowBuckets);
        }

        public synchronized ArrayList<Long> getSlowReceivedSeries()
        {
            manageBuckets();
            return getReceivedSeries(this.m_slowBuckets);
        }

        public synchronized ArrayList<Long> getFastSentSeries()
        {
            manageBuckets();
            return getSentSeries(this.m_fastBuckets);
        }

        public synchronized ArrayList<Long> getFastReceivedSeries()
        {
            manageBuckets();
            return getReceivedSeries(this.m_fastBuckets);
        }
		public  String byteCountToDisplaySize(long bytes, boolean si)
		{
			// http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java/3758880#3758880
			int unit = si ? 1000 : 1024;
			if (bytes < unit) return bytes + " B";
			int exp = (int) (Math.log(bytes) / Math.log(unit));
			String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
			return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}
		public synchronized long getBytesSent() {
			long xSent = TrafficStats.getTotalTxBytes();
			mSent = xSent - mLastSent;
			mLastSent = xSent;
			return this.mSent;
		}

		public synchronized long getBytesReceived()
		{
			long xReceived = TrafficStats.getTotalRxBytes();
			mReceived = xReceived - mLastReceived;
			mLastReceived = xReceived;
			return this.mReceived;

		}
	}
}
