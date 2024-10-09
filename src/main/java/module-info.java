module com.tenzens.charliefx {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires org.eclipse.jetty.client;
    requires org.eclipse.jetty.http;
    requires java.sql.rowset;

    exports com.tenzens.charliefx;
    exports com.tenzens.charliefx.Repository.Entries.RemoteDataSource;
    exports com.tenzens.charliefx.Repository.Entries;
    exports com.tenzens.charliefx.Repository.Model;

    opens com.tenzens.charliefx.Repository.Model to com.google.gson;
    opens com.tenzens.charliefx.Repository.Authentication to com.google.gson;
    opens com.tenzens.charliefx.Repository.Entries.LocalDataSource to com.google.gson;
    opens com.tenzens.charliefx.Repository.Entries.RemoteDataSource to com.google.gson;
    opens com.tenzens.charliefx.Repository.Authentication.LocalDataSource to com.google.gson;
    opens com.tenzens.charliefx.Repository.Authentication.RemoteDataSource to com.google.gson;

    opens com.tenzens.charliefx to com.google.gson, javafx.fxml;
    opens com.tenzens.charliefx.Views to com.google.gson, javafx.fxml;
    opens com.tenzens.charliefx.Repository to com.google.gson, javafx.fxml;
    exports com.tenzens.charliefx.Repository.Preferences.Model;
    opens com.tenzens.charliefx.Repository.Preferences.Model to com.google.gson;
    exports com.tenzens.charliefx.Repository.Entries.Model;
    opens com.tenzens.charliefx.Repository.Entries.Model to com.google.gson;
}