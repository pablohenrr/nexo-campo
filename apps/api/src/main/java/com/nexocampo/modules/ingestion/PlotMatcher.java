package com.nexocampo.modules.ingestion;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encontra o talhão citado em um texto. Aceita o nome exato, o nome contido na frase ou só o número
 * ("talhão 4" encontra "Talhão 04 - Milho Safrinha").
 */
final class PlotMatcher {

    private static final Pattern PLOT_NUMBER_IN_TEXT = Pattern.compile("talhao\\s*(?:n[o.]?\\s*)?0*(\\d+)");
    private static final Pattern FIRST_NUMBER = Pattern.compile("0*(\\d+)");

    private PlotMatcher() {
    }

    static Optional<String> find(String reference, List<String> plotNames) {
        if (reference == null || reference.isBlank() || plotNames.isEmpty()) {
            return Optional.empty();
        }
        String normalizedReference = normalize(reference);

        Optional<String> exact = single(plotNames.stream()
                .filter(name -> normalize(name).equals(normalizedReference))
                .toList());
        if (exact.isPresent()) {
            return exact;
        }

        Optional<String> contained = single(plotNames.stream()
                .filter(name -> normalizedReference.contains(normalize(name)))
                .toList());
        if (contained.isPresent()) {
            return contained;
        }

        Matcher referenceNumber = PLOT_NUMBER_IN_TEXT.matcher(normalizedReference);
        if (!referenceNumber.find()) {
            return Optional.empty();
        }
        String number = referenceNumber.group(1);
        return single(plotNames.stream()
                .filter(name -> number.equals(firstNumber(name)))
                .toList());
    }

    private static Optional<String> single(List<String> candidates) {
        return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
    }

    private static String firstNumber(String plotName) {
        Matcher matcher = FIRST_NUMBER.matcher(plotName);
        return matcher.find() ? matcher.group(1) : null;
    }

    static String normalize(String value) {
        // NFKD também separa símbolos como "º" ("talhão nº 4" vira "talhao no 4").
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
