package EdDYON.guaniao.client.guide;

import com.google.gson.Gson;
import java.io.Reader;
import java.util.List;
import java.util.Locale;

/** Versioned, offline content. Web markup is converted at authoring time, never executed in game. */
record HandbookData(int schema, List<Page> pages, List<Category> categories) {
    static HandbookData read(Reader reader) {
        HandbookData data = new Gson().fromJson(reader, HandbookData.class);
        if (data == null || data.schema != 1 || data.pages == null || data.categories == null)
            throw new IllegalArgumentException("Invalid handbook data");
        return data;
    }

    Page page(String id) { return pages.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null); }

    List<Page> find(String kind, String category, String query) {
        String[] words = query.strip().toLowerCase(Locale.ROOT).split("\\s+");
        return pages.stream().filter(p -> p.catalog && (kind.isEmpty() || p.kind.equals(kind)))
                .filter(p -> category.isEmpty() || category.equals(p.category))
                .filter(p -> {
                    String text = p.searchText();
                    for (String word : words) if (!text.contains(word)) return false;
                    return true;
                }).toList();
    }

    record Category(String id, String title) {}
    record Fact(String label, String text) {}
    record Span(String text, String link) {}
    record Block(String type, String id, List<Span> spans, String recipe, String sound, String book) {}
    record Section(String id, String title, List<Block> blocks) {}
    record Page(String id, String route, String kind, String category, String model, String icon,
                boolean catalog, String title, String summary, List<Fact> facts, List<Section> sections) {
        String searchText() {
            StringBuilder text = new StringBuilder(id).append(' ').append(title).append(' ').append(summary);
            for (Fact fact : facts) text.append(' ').append(fact.text);
            for (Section section : sections) {
                text.append(' ').append(section.title);
                for (Block block : section.blocks) for (Span span : block.spans) text.append(' ').append(span.text);
            }
            return text.toString().toLowerCase(Locale.ROOT);
        }
    }
}
