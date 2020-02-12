import com.micronet.dsc.ats.Log;

import org.junit.Test;

public class RBClearerTesting {
    @Test
    public void isTimeToPerformCleaningTesting(){
        boolean timeToClean = false;
        long lastCleanUpTime = 1581374892426L;
        long currentSystemTime = System.currentTimeMillis();
        int targetCleaningTime = 10;
        long targetCleaningTimeMillionSeconds = lastCleanUpTime + ((long)targetCleaningTime * 86400000);
        System.out.println("currentSystemTime: "+currentSystemTime);
        System.out.println("targetCleaningTimeMillionSeconds: "+targetCleaningTimeMillionSeconds);
        double difference = (double) currentSystemTime - (double) targetCleaningTimeMillionSeconds;
        System.out.println("difference: "+difference);
        double seconds = difference/1000.0;
        double minutes = seconds/60.0;
        double hours = minutes/60.0;
        double days = hours/24.0;
        System.out.println("Days = " + days);

        if(days>=1.0){
            timeToClean = true;
        }

        System.out.println("timeToClean: " + timeToClean);
    }
}
