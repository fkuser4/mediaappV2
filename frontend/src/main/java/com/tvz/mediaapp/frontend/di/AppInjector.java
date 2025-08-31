package com.tvz.mediaapp.frontend.di;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AppInjector {
    private static Injector injector;

    public static void createInjector() { if (injector == null) injector = Guice.createInjector(new AppModule()); }

    public static Injector getInjector() { return injector; }
}