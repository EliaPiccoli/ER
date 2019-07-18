package System;

import Component.MonitoringComponent;
import Entities.Patient;
import Entities.Recovery;
import Entities.User;
import Generator.DataThread;
import Generator.Value;
import State.Reducer;
import State.ReducerString;
import State.StringCommand;
import State.State;
import State.Store;
import State.DatabaseService;
import State.MiddlewareString;
import State.Middleware;
import Component.HPComponent;
import Component.AlarmsComponent;

import Component.LoginComponent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import Main.Tuple;
import java.util.*;
import java.util.stream.Collectors;

public class Sistema {
    private static Sistema s;
    private Store<StringCommand> store;
    private InterfacesController controller;
    private Stage monitoringStage = null;
    private Stage alarmStage = null;

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
                .with("GENERATE_BP", (c, s) -> {
                    s.getActiveRecoveries().forEach(r -> r.generateMonitoring(Value.BP));
                    return s;
                })
                .with("GENERATE_HEART_RATE", (c, s) -> {
                    s.getActiveRecoveries().forEach(r -> r.generateMonitoring(Value.HEART_RATE));
                    return s;
                })
                .with("GENERATE_TEMPERATURE", (c, s) -> {
                    s.getActiveRecoveries().forEach(r -> r.generateMonitoring(Value.TEMPERATURE));
                    return s;
                })
                .with("LOAD")
                .with("ADD_PATIENT")
                .with("START_MONITORING")               
                .with("SHOW_MONITORING")
                .with("STOP_MONITORING")
                .with("CLOSE_MONITORING")
                .with("SHOW_ALARMS")
                .with("GET_LOGIN", (c, s) -> {
                    if(s.getUser().isValid())
                        controller.toFront();
                    else controller.activate("login", LoginComponent.loginTitle);
                    return s;
                });
        Middleware<StringCommand> middleware = new MiddlewareString(monitoringStage)
                .with("LOGIN", (c, s, m) -> {
                    User u = (User) c.getArg();
                    if (s.getUserCheck().equals(u)) {
                        s.setUser(s.getUserCheck());
                        return new Tuple<>(new StringCommand("LOGIN_SUCCESS"), s);
                    }
                    else {
                        return new Tuple<>(new StringCommand("LOGIN_FAILURE"), s);
                    }
                })
                .with("LOAD", (c, s, m) -> {
                    List<Patient> ps = DatabaseService.getEntries("Patient").stream()
                            .map(e -> (Patient) e)
                            .collect(Collectors.toList());
                    s.setPatients(ps);
                    List<Recovery> rec = ps.stream().flatMap(p -> p.getRecoveries().stream()).filter(Recovery::isActive)
                            .collect(Collectors.toList());
                    //TODO add
                    /*if (rec.size() == 0) {

                    }*/
                    s.setMainRecoveryIndex(0);
                    return new Tuple<>(new StringCommand("LOADED"), s);
                })
                .with("ADD_PATIENT" , (c, s, m) -> {
                    Patient patient = (Patient) c.getArg();
                    s.addPatient(patient);
                    DatabaseService.addEntry(patient);
                    return new Tuple<>(new StringCommand("ADDED_PATIENT"), s);
                    })
                .with("SHOW_MONITORING", (c,s,m) -> {
                    if (monitoringStage == null) {
                        monitoringStage = new Stage();
                        monitoringStage.getIcons().add(new Image("/logo.png"));
                        monitoringStage.setScene(new Scene(getInterface("MON")));
                        monitoringStage.setTitle(MonitoringComponent.monitoringTitle);
                        monitoringStage.sizeToScene();
                    }
                    monitoringStage.show();
                    monitoringStage.toFront();
                    return new Tuple<>((new StringCommand("OPEN_MONITORING")), s);
                })
                .with("START_MONITORING", (c,s,m) -> {
                    new DataThread(store).start();
                    return new Tuple<>(new StringCommand("MONITORING_HAS_STARTED"), s);
                })
                .with("STOP_MONITORING", (c,s,m) -> {
                    ((MiddlewareString) m).getMonitoring().interrupt();
                    return new Tuple<>(new StringCommand("STOPPED_MONITORING"), s);
                })
                .with("CLOSE_MONITORING", (c,s,m) -> {
                    monitoringStage.close();
                    return new Tuple<>(new StringCommand("CLOSE_MONITORING"), s);
                })
                .with("SHOW_ALARMS", (c,s,m) -> {
                    if (alarmStage == null) {
                        alarmStage = new Stage();
                        alarmStage.getIcons().add(new Image("/logo.png"));
                        alarmStage.setScene(new Scene(getInterface("ALM")));
                        alarmStage.setTitle(AlarmsComponent.AlarmsTitle);
                        alarmStage.sizeToScene();
                    }
                    alarmStage.show();
                    alarmStage.toFront();
                    return new Tuple<>(new StringCommand("SHOW_ALARMS"), s);
                });

        store = new Store<StringCommand>(new State(), reducer, middleware);
        store.update(new StringCommand("LOAD"));
        store.update(new StringCommand("START_MONITORING"));
        /*store.update(new StringCommand("ADD_PATIENT", new Patient("Roberto", "Posenato", "PSNRBRA373UUS88I",
                "Verona", new GregorianCalendar(1981, Calendar.FEBRUARY, 11).getTime())));*/
    }

    public void setupUI(Stage stage){
        try {
            stage.getIcons().add(new Image("/logo.png"));
            this.controller = new InterfacesController(stage);
            this.controller.addInterface("login", new LoginComponent<StringCommand>().getLoader().load());
            this.controller.addInterface("HPDF", new HPComponent<StringCommand>("default").getLoader().load());
            this.controller.addInterface("HPS", new HPComponent<StringCommand>("search").getLoader().load());
            this.controller.addInterface("HPM", new HPComponent<StringCommand>("monitoring").getLoader().load());
            this.controller.addInterface("HPD", new HPComponent<StringCommand>("dismiss").getLoader().load());
            this.controller.addInterface("MON", new MonitoringComponent<StringCommand>().getLoader().load());
            this.controller.addInterface("ALM", new AlarmsComponent<StringCommand>().getLoader().load());
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

    public Pane getInterface(String s) {
        return this.controller.getInterface(s);
    }

    public Stage getMonitoringStage() {
        return monitoringStage;
    }
}
