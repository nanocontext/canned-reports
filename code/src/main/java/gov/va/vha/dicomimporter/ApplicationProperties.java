package gov.va.vha.dicomimporter;

import java.io.IOException;
import java.util.Properties;

public class ApplicationProperties extends Properties {
    private static ApplicationProperties singleton;

    static {
        singleton = new ApplicationProperties();
    }

    public static final ApplicationProperties getSingleton() {
        return singleton;
    }

    private ApplicationProperties() {
        try {
            this.load(ApplicationProperties.class.getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load application properties");
        }
    }
}
