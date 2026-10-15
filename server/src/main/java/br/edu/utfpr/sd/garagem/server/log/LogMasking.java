package br.edu.utfpr.sd.garagem.server.log;

/** Mantido por compatibilidade — delega para {@code common}. */
public final class LogMasking {

    private LogMasking() {
    }

    public static String maskPassword(String rawJson) {
        return br.edu.utfpr.sd.garagem.common.log.LogMasking.maskPassword(rawJson);
    }
}
