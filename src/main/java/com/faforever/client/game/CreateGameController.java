package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import com.google.common.base.Strings;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.RangeSlider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Objects;

public class CreateGameController {

  private static final String DEFAULT_MOD = "faf";

  @FXML
  Button createGameButton;

  @FXML
  Label mapNameLabel;

  @FXML
  TextField mapSearchTextField;

  @FXML
  ImageView mapImageView;

  @FXML
  TextField titleTextField;

  @FXML
  CheckListView<GameTypeBean> modListView;

  @FXML
  TextField passwordTextField;

  @FXML
  TextField minRankingTextField;

  @FXML
  RangeSlider rankingSlider;

  @FXML
  TextField maxRankingTextField;

  @FXML
  ComboBox<GameTypeBean> gameTypeComboBox;

  @FXML
  ListView<MapInfoBean> mapListView;

  @Autowired
  Environment environment;

  @Autowired
  MapService mapService;

  @Autowired
  ModService modService;

  @Autowired
  GameService gameService;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  I18n i18n;

  @FXML
  Node createGameRoot;

  private FilteredList<MapInfoBean> filteredMaps;

  @FXML
  void initialize() {
    mapSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.isEmpty()) {
        filteredMaps.setPredicate(mapInfoBean -> true);
      } else {
        filteredMaps.setPredicate(mapInfoBean -> mapInfoBean.getName().toLowerCase().contains(newValue.toLowerCase()));
      }
      if (!filteredMaps.isEmpty()) {
        mapListView.getSelectionModel().select(0);
      }
    });
    mapSearchTextField.setOnKeyPressed(event -> {
      MultipleSelectionModel<MapInfoBean> selectionModel = mapListView.getSelectionModel();
      int currentMapIndex = selectionModel.getSelectedIndex();
      int newMapIndex = currentMapIndex;
      if (KeyCode.DOWN == event.getCode()) {
        if (filteredMaps.size() > currentMapIndex + 1) {
          newMapIndex++;
        }
        event.consume();
      } else if (KeyCode.UP == event.getCode()) {
        if (currentMapIndex > 0) {
          newMapIndex--;
        }
        event.consume();
      }
      selectionModel.select(newMapIndex);
      mapListView.scrollTo(newMapIndex);
    });

    gameTypeComboBox.setCellFactory(param -> new ModListCell());
    gameTypeComboBox.setButtonCell(new ModListCell());
  }

  @PostConstruct
  void postConstruct() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      preferencesService.addUpdateListener(preferences -> {
        if (preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
          init();
        }
      });
    } else {
      init();
    }
  }

  private void init() {
    initRankingSlider();
    initModList();
    initMapSelection();
    initGameTypeComboBox();
    setLastGameTitle();
    selectLastMap();
  }

  private void setLastGameTitle() {
    titleTextField.setText(preferencesService.getPreferences().getLastGameTitle());
  }

  private void selectLastMap() {
    String lastMap = preferencesService.getPreferences().getLastMap();
    for (MapInfoBean mapInfoBean : mapListView.getItems()) {
      if (mapInfoBean.getName().equals(lastMap)) {
        mapListView.getSelectionModel().select(mapInfoBean);
        break;
      }
    }
  }

  private void initGameTypeComboBox() {
    gameService.addOnGameTypeInfoListener(change -> {
      change.getValueAdded();

      gameTypeComboBox.getItems().add(change.getValueAdded());
      selectLastOrDefaultGameType();
    });
  }

  private void selectLastOrDefaultGameType() {
    String lastGameMod = preferencesService.getPreferences().getLastGameType();
    if (lastGameMod == null) {
      lastGameMod = DEFAULT_MOD;
    }

    for (GameTypeBean mod : gameTypeComboBox.getItems()) {
      if (Objects.equals(mod.getName(), lastGameMod)) {
        gameTypeComboBox.getSelectionModel().select(mod);
        break;
      }
    }
  }

  private void initModList() {
    modListView.setItems(modService.getInstalledMods());
    modListView.setCellFactory(param -> new ModListCell());
  }

  private void initMapSelection() {
    ObservableList<MapInfoBean> localMaps = mapService.getLocalMaps();

    filteredMaps = new FilteredList<>(localMaps);

    mapListView.setItems(filteredMaps);
    mapListView.setCellFactory(mapListCellFactory());
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        mapNameLabel.setText("");
        return;
      }
      String mapName = newValue.getName();

      mapNameLabel.setText(mapName);
      mapImageView.setImage(mapService.loadLargePreview(mapName));
      preferencesService.getPreferences().setLastMap(mapName);
      preferencesService.storeInBackground();
    });

    String lastMap = preferencesService.getPreferences().getLastMap();
    if (lastMap != null) {
      for (MapInfoBean map : localMaps) {
        mapListView.getSelectionModel().select(map);
      }
    }
  }

  private void initRankingSlider() {
    rankingSlider.setFocusTraversable(false);
    rankingSlider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() < rankingSlider.getHighValue()) {
        minRankingTextField.setText(String.valueOf(newValue.intValue()));
      }
    });
    rankingSlider.highValueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() > rankingSlider.getLowValue()) {
        maxRankingTextField.setText(String.valueOf(newValue.intValue()));
      }
    });
    minRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      rankingSlider.setLowValue(Double.parseDouble(newValue));
    });
    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      rankingSlider.setHighValue(Double.parseDouble(newValue));
    });

    rankingSlider.setMax(environment.getProperty("rating.max", Integer.class));
    rankingSlider.setMin(environment.getProperty("rating.min", Integer.class));
    rankingSlider.setHighValue(environment.getProperty("rating.selectedMax", Integer.class));
    rankingSlider.setLowValue(environment.getProperty("rating.selectedMin", Integer.class));

    titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameTitle(newValue);
      preferencesService.storeInBackground();
    });
  }

  @FXML
  void onRandomMapButtonClicked(ActionEvent event) {
    int mapIndex = (int) (Math.random() * filteredMaps.size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  @FXML
  void onCreateButtonClicked(ActionEvent event) {
    if (StringUtils.isEmpty(titleTextField.getText())) {
      // TODO tell the user
      return;
    }

    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        Strings.emptyToNull(passwordTextField.getText()),
        gameTypeComboBox.getSelectionModel().getSelectedItem().getName(),
        mapListView.getSelectionModel().getSelectedItem().getName(),
        0);

    gameService.hostGame(newGameInfo, new Callback<Void>() {
          @Override
          public void success(Void result) {
            // FIXME do something or remove the callback
          }

          @Override
          public void error(Throwable e) {
            // FIXME do something or remove the callback
          }
        }
    );
  }

  public Node getRoot() {
    return createGameRoot;
  }

  @NotNull
  private javafx.util.Callback<ListView<MapInfoBean>, ListCell<MapInfoBean>> mapListCellFactory() {
    return param -> new ListCell<MapInfoBean>() {
      @Override
      protected void updateItem(MapInfoBean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item.getName());
        }
      }
    };
  }
}
