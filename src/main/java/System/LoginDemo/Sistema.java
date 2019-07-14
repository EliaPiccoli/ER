package System.LoginDemo;

import State.Entities.Entry;
import State.Entities.Patient;
import State.Entities.User;
import State.Reducer;
import State.ReducerString;
import State.StringCommand;
import State.State;
import State.Store;
import State.DatabaseService;
import State.MiddlewareString;
import State.Middleware;
import System.LoginDemo.HP.HPComponent;

import javafx.stage.Stage;
import Main.Tuple;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Sistema {
    private static Sistema s;
    private Store<StringCommand> store;
    private InterfacesController controller;
    public static Sistema getInstance() {
        if (s == null)
            s = new Sistema();
        return s;
    }

    //will have all the information into the state and iterate over them
    //NB. This is just demo environment
    private Sistema() {
        Reducer<StringCommand> reducer = new ReducerString()
                .with("LOGIN")
                .with("LOGOUT", (c, s) -> {
                    s.setUser(new User());
                    return s;
                })
                .with("LOAD");
        Middleware<StringCommand> middleware = new MiddlewareString()
                .with("LOGIN", (c, s, m) -> {
                    User u = (User) c.getArg();
                    if (s.getUserCheck().equals(u)) {
                        s.setUser(s.getUserCheck());
                        return new Tuple<>(new StringCommand("LOGIN_SUCCESS", UUID.randomUUID()), s);
                    }
                    else {
                        return new Tuple<>(new StringCommand("LOGIN_FAILURE", UUID.randomUUID()), s);
                    }
                })
                .with("LOAD", (c, s, m) -> {
                    List<Patient> ps = DatabaseService.getEntries("Patient").stream()
                            .map(e -> (Patient) e)
                            .collect(Collectors.toList());
                    s.setPatients(ps);
                    return new Tuple<>(new StringCommand("LOADED", UUID.randomUUID()), s);
                });
        store = new Store<StringCommand>(new State(), reducer, middleware);
        store.update(new StringCommand("LOAD", UUID.randomUUID()));
    }

    public void setupUI(Stage stage){
        try {
            this.controller = new InterfacesController(stage);
            this.controller.addInterface("login", new LoginComponent<StringCommand>().getLoader().load());
            this.controller.addInterface("HPDF", new HPComponent<StringCommand>("default").getLoader().load());
            this.controller.addInterface("HPS", new HPComponent<StringCommand>("search").getLoader().load());
            this.controller.addInterface("HPM", new HPComponent<StringCommand>("monitoring").getLoader().load());
            this.controller.addInterface("HPD", new HPComponent<StringCommand>("dismiss").getLoader().load());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during interfaces setup");
        }
    }

    public Store getStore() {
        return store;
    }

    public void setInterface(String component, String title) {
        controller.activate(component, title);
    }

    public void endSystem() {
        this.controller.deactivate();
    }
}