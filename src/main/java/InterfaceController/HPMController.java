package InterfaceController;


import Entities.Administration;
import Entities.Monitoring;
import Entities.Patient;
import Entities.Recovery;
import State.State;
import State.StateEvent;
import State.Store;
import State.StringCommand;
import Component.HPComponent;
import Component.LoginComponent;
import System.Sistema;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.Subject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HPMController {
    private final Store<StringCommand> store;
    private final Sistema sys = Sistema.getInstance();
    @FXML private TableView<Monitoring> tableMonitorings = new TableView<>();
    @FXML private TableView<Administration> tableAdministrations = new TableView<>();
    @FXML private ComboBox<Recovery> patientComboBox;
    @FXML private TableColumn<Administration, String> drugColumn;
    private Disposable dis;

    public HPMController(Store<StringCommand> store, Subject<StateEvent> stream) {
        this.store = store;

        try {
            dis.dispose();
        } catch (NullPointerException e) {}

        dis = stream.subscribe(se ->
        {
            updatePatients(se.state());
            setData(patientComboBox.getValue());
        });
    }

    @FXML protected void initialize() {
        patientComboBox.setConverter(new StringConverter<Recovery>() {
            @Override
            public String toString(Recovery recovery) {
                try {
                    return recovery.getPatient().getName() + " " + recovery.getPatient().getSurname();
                } catch (Exception err) {
                    return "";
                }
            }

            @Override
            public Recovery fromString(String s) {
                return patientComboBox.getValue();
            }
        });

        //todo this is wrong
        /*drugColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Administration , String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Administration , String> param) {
                return new SimpleObjectProperty<>(param.getValue().getPrescription().getDrug());

            }
        });*/

        initialize(store.poll());
    }

    @FXML protected void initialize(State state) {
        updatePatients(state);
        patientComboBox.getSelectionModel().selectFirst();
        setData(patientComboBox.getValue());
    }

    @FXML protected void updatePatients(State state) {
        List<Recovery> rec = state.getActiveRecoveries();
        ObservableList<Recovery> data = this.patientComboBox.getItems();
        int index = patientComboBox.getSelectionModel().getSelectedIndex();
        data.removeAll(data);
        data.addAll(rec);
        patientComboBox.getSelectionModel().select(index);
    }

    @FXML protected void showMonitoring() {
        store.update(new StringCommand("SHOW_MONITORING"));
        store.update(new StringCommand("START_MONITORING"));
    }
    @FXML protected void selectedPatient() {
        this.setData(patientComboBox.getValue());
    }
    @FXML protected void setData(Recovery r) {
        if (r != null) {
            List<Monitoring> monitorings = r.getMonitorings();
            List<Administration> administrations = r.getPatient().getAdministrations();
            ObservableList<Monitoring> data1 = tableMonitorings.getItems();
            data1.add(0,r.getLastMonitoring());
            data1.addAll(monitorings);

            ObservableList<Administration> data2 = tableAdministrations.getItems();
            data2.removeAll(data2);
            data2.addAll(administrations);
        }
    }
    @FXML protected void search() {
        sys.setInterface("HPS", HPComponent.HPTitle);
    }
    @FXML protected void dismissPatient() {
        sys.setInterface("HPD", HPComponent.HPTitle);
    }
    @FXML protected void showLast2H() {
        sys.setInterface("HPM", HPComponent.HPTitle);
    }

    @FXML protected void logout() {
        store.update(new StringCommand("LOGOUT"));
        sys.setInterface("login", LoginComponent.loginTitle);
    }

    @FXML protected void close() {
        sys.endSystem();
    }
}
