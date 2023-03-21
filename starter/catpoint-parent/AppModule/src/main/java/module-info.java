module AppModule {
    exports com.udacity.catpoint.data;
    opens com.udacity.catpoint.data to com.google.gson;

    requires java.desktop;
    requires com.miglayout.swing;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;


}