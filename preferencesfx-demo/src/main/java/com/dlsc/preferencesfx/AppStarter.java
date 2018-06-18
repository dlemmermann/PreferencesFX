package com.dlsc.preferencesfx;

// Change this import depending on the demo
import com.dlsc.preferencesfx.standard.RootPane;
//import com.dlsc.preferencesfx.i18n.RootPane;
//import com.dlsc.preferencesfx.oneCategory.RootPane;
//import com.dlsc.preferencesfx.extended.RootPane;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class AppStarter extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    RootPane rootPane = new RootPane();
    Scene myScene = new Scene(rootPane);

    primaryStage.setTitle("PreferencesFx Demo");
    primaryStage.setScene(myScene);
    primaryStage.setWidth(1000);
    primaryStage.setHeight(700);
    primaryStage.show();
    primaryStage.centerOnScreen();
  }

  public AppStarter() {
    addPreferencesListener();
  }

  private void addPreferencesListener() {
    Preferences prefs = Preferences.userNodeForPackage(AppStarter.class);
    prefs.addPreferenceChangeListener(evt -> {
      System.out.println("Preference has changed: " + evt.getKey() + " -> " + evt.getNewValue());
    });
  }
}
