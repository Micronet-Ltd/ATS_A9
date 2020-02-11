import org.junit.Test;

public class RBClearerTesting {
    @Test
    public void isTimeToPerformCleaningTesting(){
        long lastCleanUpTime = 1581374892426L;
        long currentSystemTime = System.currentTimeMillis();
        System.out.println(currentSystemTime);
        double difference = (double) currentSystemTime - (double) lastCleanUpTime;
        System.out.println(difference);
    }
}
