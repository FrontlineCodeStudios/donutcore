package ro.andreilarazboi.donutcore.sell;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public final class Utils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Utils() {
    }

    public static String formatColors(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer(input.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00a7').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)buffer.toString());
    }

    public static List<String> formatColors(List<String> lines) {
        return lines.stream().map(Utils::formatColors).collect(Collectors.toList());
    }

    public static String abbreviateNumber(double number) {
        int unitIndex;
        if (number < 1000.0) {
            if (number == (double)((long)number)) {
                return String.format("%d", (long)number);
            }
            return String.format("%.1f", number);
        }
        String[] units = new String[]{"K", "M", "B", "T", "Q"};
        double value = number;
        for (unitIndex = -1; value >= 1000.0 && unitIndex < units.length - 1; value /= 1000.0, ++unitIndex) {
        }
        String formatted = value == (double)((long)value) ? String.format("%d", (long)value) : String.format("%.2f", value);
        return formatted + units[unitIndex];
    }

    /**
     * Parses a number that may include a shorthand suffix.
     * Supported suffixes (case-insensitive): k=1,000  m=1,000,000  b=1,000,000,000  t=1e12  q=1e15
     * Examples: "30k" → 30000, "1.5m" → 1500000, "30" → 30
     *
     * @param input the raw input string
     * @return the parsed double value
     * @throws NumberFormatException if the input cannot be parsed
     */
    public static double parseShorthandNumber(String input) {
        if (input == null || input.isBlank()) {
            throw new NumberFormatException("empty input");
        }
        String trimmed = input.trim().toLowerCase(java.util.Locale.ROOT);
        double multiplier = 1.0;
        if (trimmed.endsWith("q")) {
            multiplier = 1_000_000_000_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("m")) {
            multiplier = 1_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("k")) {
            multiplier = 1_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return Double.parseDouble(trimmed) * multiplier;
    }
}

