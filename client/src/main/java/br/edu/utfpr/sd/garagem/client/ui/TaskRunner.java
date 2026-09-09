package br.edu.utfpr.sd.garagem.client.ui;

import javafx.concurrent.Task;

/** Roda uma {@link Task} numa thread daemon nomeada, longe da JavaFX Application Thread. */
final class TaskRunner {

    private TaskRunner() {
    }

    static void runInBackground(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }
}
