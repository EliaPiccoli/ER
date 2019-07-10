package State;

import Main.Tuple;

import java.util.HashMap;

public class MiddlewareString implements Middleware<StringCommand> {
    private HashMap<String, TriFunction<StringCommand, State, Middleware<StringCommand>, Tuple<StringCommand, State>>> map =
            new HashMap<String, TriFunction<StringCommand, State, Middleware<StringCommand>, Tuple<StringCommand, State>>>();

    @Override
    public Middleware<StringCommand> with(String st, TriFunction<StringCommand, State,
            Middleware<StringCommand>, Tuple<StringCommand, State>> fun) {
        this.map.put(st, fun);
        return this;
    }

    @Override
    public Tuple<StringCommand, State> run(State s, StringCommand stringCommand) {
        TriFunction<StringCommand, State, Middleware<StringCommand>, Tuple<StringCommand, State>> fun =
                this.map.get(stringCommand.name());
        return fun.apply(stringCommand, s, this);
    }
    public boolean check(String s) {
        return this.map.containsKey(s);
    }
}
