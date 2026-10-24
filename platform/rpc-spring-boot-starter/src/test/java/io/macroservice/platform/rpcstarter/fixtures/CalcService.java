package io.macroservice.platform.rpcstarter.fixtures;

import io.macroservice.platform.support.ModuleService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleService(module = "calc")
public class CalcService implements CalcApi {

    public final List<String> fired = new CopyOnWriteArrayList<>();
    /** Every message fired by any instance; the bean in a context is wrapped, so tests cannot reach the instance. */
    public static final List<String> FIRED_ANYWHERE = new CopyOnWriteArrayList<>();

    public int add(int a, int b) {
        return a + b;
    }

    public Echo echo(Echo echo) {
        return echo;
    }

    public void fire(String message) {
        fired.add(message);
        FIRED_ANYWHERE.add(message);
    }

    public String boom() {
        throw new IllegalStateException("secret internal detail");
    }

    public String denied(String who) {
        throw new DeniedException("denied: " + who);
    }

    public String nullable(String text) {
        return text;
    }
}
