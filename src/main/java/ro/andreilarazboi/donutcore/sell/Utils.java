package ro.andreilarazboi.donutcore.sell;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

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
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return translateAmpersandCodes(buffer.toString());
    }

    private static String translateAmpersandCodes(String input) {
        char[] chars = input.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                if ("0123456789abcdefklmnorx".indexOf(code) >= 0) {
                    sb.append('§').append(code);
                    i++;
                    continue;
                }
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public static Component toComponent(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacySection().deserialize(formatColors(text));
    }

    public static List<Component> toComponents(List<String> lines) {
        return lines.stream().map(Utils::toComponent).collect(Collectors.toList());
    }

    public static List<String> formatColors(List<String> lines) {
        return lines.stream().map(Utils::formatColors).collect(Collectors.toList());
    }

    public static String stripColor(String s) {
        if (s == null) return null;
        return s.replaceAll("§[0-9a-fA-Fk-oK-OrRxX]", "");
    }

    public static String stripColor(Component component) {
        if (component == null) return "";
        return stripColor(LegacyComponentSerializer.legacySection().serialize(component));
    }

    public static Sound resolveSound(String name, Sound fallback) {
        if (name == null || name.isEmpty()) return fallback;
        String lower = name.toLowerCase(Locale.ROOT);
        Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(lower));
        if (s != null) return s;
        String dotted = lower.replace('_', '.');
        s = Registry.SOUNDS.get(NamespacedKey.minecraft(dotted));
        if (s != null) return s;
        return fallback;
    }

    public static String abbreviateNumber(double number) {
        if (number < 1000.0) {
            if (number == (double) ((long) number)) {
                return String.format("%d", (long) number);
            }
            return String.format("%.1f", number);
        }
        String[] units = new String[]{"K", "M", "B", "T", "Q"};
        double value = number;
        int unitIndex = -1;
        for (; value >= 1000.0 && unitIndex < units.length - 1; value /= 1000.0, ++unitIndex) {
        }
        String formatted = value == (double) ((long) value) ? String.format("%d", (long) value) : String.format("%.2f", value);
        return formatted + units[unitIndex];
    }
}
