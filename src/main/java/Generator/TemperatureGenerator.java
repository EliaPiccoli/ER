package Generator;

import Main.Tuple;
import State.Store;
import State.StringCommand;
import System.Sistema;

import java.util.Random;

public class TemperatureGenerator implements GeneratorInterface {
    private boolean canGenerateAlarm = true;
    private final Random random = new Random();
    private double mean = 36.75;
    private double variance = 0.0625;

    public TemperatureGenerator() {
    }

    public void evolve(Sickness sick) {
        if(sick == Sickness.IPERTERMIA) {
            mean = 38.75;
            variance = 0.0625;
        } else if(sick == Sickness.IPOTERMIA) {
            mean = 35.0;
            variance = 0.0625;
        }
        canGenerateAlarm = false;
    }

    public void reset() {
        canGenerateAlarm = true;
        mean = 36.75;
        variance = 0.0625;
    }

    public Double getValue() {
        double x =  mean + random.nextGaussian()*variance;
        if(canGenerateAlarm) {
            Store<StringCommand> store = Sistema.getInstance().getStore();
            if(x < 36) {
                canGenerateAlarm = false;
                store.update(new StringCommand("ALARM_ACTIVATED", new Tuple<>(2, Sickness.IPOTERMIA)));
            } else if(x > 37.5) {
                canGenerateAlarm = false;
                store.update(new StringCommand("ALARM_ACTIVATED", new Tuple<>(2, Sickness.IPERTERMIA)));
            }
        }

        return x;
    }
}
