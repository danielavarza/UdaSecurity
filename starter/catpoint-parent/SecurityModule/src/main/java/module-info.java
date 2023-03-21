module SecurityModule {
    exports com.udacity.catpoint.security;
    exports com.udacity.catpoint.application;

    requires AppModule;
    requires ImageModule;
    requires java.desktop;
    requires com.miglayout.swing;
}