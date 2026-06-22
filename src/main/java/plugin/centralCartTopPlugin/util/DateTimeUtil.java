package plugin.centralCartTopPlugin.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Centraliza o parsing e a formatação das datas vindas da API CentralCart.
 *
 * <p>A API retorna {@code created_at} no formato {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
 * (ex.: {@code 2026-06-21T23:26:58.000-03:00}). Mantemos parsers legados como fallback para
 * tolerar variações de formato entre versões da API.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final DateTimeFormatter[] LEGACY_PARSERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    };

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Tenta converter a string de data da API em {@link LocalDateTime}.
     *
     * @param raw valor bruto de {@code created_at} (pode ser nulo)
     * @return o {@link LocalDateTime} correspondente ou {@code null} se nenhum formato casar
     */
    public static LocalDateTime tryParse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        // Formato principal atual da API: ISO com offset (-03:00)
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // segue para os formatos legados
        }

        for (DateTimeFormatter fmt : LEGACY_PARSERS) {
            try {
                return LocalDateTime.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato
            }
        }
        return null;
    }
}
