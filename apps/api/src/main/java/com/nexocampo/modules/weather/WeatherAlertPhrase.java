package com.nexocampo.modules.weather;

public final class WeatherAlertPhrase {

    private WeatherAlertPhrase() {}

    public static String of(CategoriaAlertaClimatico categoria, String place) {
        String local = place == null || place.isBlank() ? "sua fazenda" : place.trim();
        String phrase = switch (categoria) {
            case CHUVA -> "NEXO Campo: chuva intensa prevista em " + local + " nas próximas 24h. Evite pulverizar.";
            case VENTO -> "NEXO Campo: vento forte previsto em " + local + " nas próximas 24h.";
            case TEMPESTADE ->
                    "NEXO Campo: tempestade prevista em " + local + " nas próximas 24h. Recolha equipamentos e pessoas.";
            case GRANIZO -> "NEXO Campo: risco de granizo em " + local + " nas próximas 24h. Proteja a lavoura.";
            case GEADA -> "NEXO Campo: geada prevista em " + local + " nas próximas 24h. Proteja a lavoura.";
            case UMIDADE_BAIXA -> "NEXO Campo: umidade baixa prevista em " + local + " nas próximas 24h.";
            case TORNADO -> "NEXO Campo: aviso de tornado em " + local + ". Procure abrigo agora.";
        };
        return phrase.length() <= 400 ? phrase : phrase.substring(0, 397) + "...";
    }
}
