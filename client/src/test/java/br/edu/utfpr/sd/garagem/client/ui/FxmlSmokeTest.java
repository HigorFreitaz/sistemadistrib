package br.edu.utfpr.sd.garagem.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlSmokeTest {

    @Test
    void mainViewCarregaSemErro() throws Exception {
        Parent root = loadOnFxThread("/fxml/main-view.fxml");
        assertNotNull(root);
    }

    @Test
    void loginViewCarregaSemErro() throws Exception {
        Parent root = loadOnFxThread("/fxml/login-view.fxml");
        assertNotNull(root);
    }

    private Parent loadOnFxThread(String resource) throws Exception {
        startToolkitIfNeeded();
        AtomicReference<Parent> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(new FXMLLoader(getClass().getResource(resource)).load());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "timeout carregando " + resource);
        assertNull(error.get(), () -> "erro ao carregar " + resource + ": " + error.get());
        return result.get();
    }

    private static boolean toolkitStarted;

    private static synchronized void startToolkitIfNeeded() {
        if (!toolkitStarted) {
            Platform.startup(() -> { });
            toolkitStarted = true;
        }
    }
}
