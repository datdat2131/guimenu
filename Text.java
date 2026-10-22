package vn.astrax.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Chuyển chuỗi &-code (và &#rrggbb) sang Component, tắt chữ nghiêng mặc định của item. */
final class Text {
    private static final LegacyComponentSerializer AMP =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private Text() {}

    static Component c(String s) {
        return AMP.deserialize(s == null ? "" : s)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    static List<Component> lines(Collection<String> in) {
        List<Component> out = new ArrayList<>(in.size());
        for (String s : in) out.add(c(s));
        return out;
    }

    static String plain(String s) {
        return PlainTextComponentSerializer.plainText().serialize(AMP.deserialize(s == null ? "" : s));
    }

    static String duration(long s) {
        if (s <= 0) return "0 giây";
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, x = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append(" ngày ");
        if (h > 0) sb.append(h).append(" giờ ");
        if (m > 0) sb.append(m).append(" phút ");
        if (x > 0 && d == 0 && h == 0) sb.append(x).append(" giây ");
        return sb.toString().trim();
    }
}
