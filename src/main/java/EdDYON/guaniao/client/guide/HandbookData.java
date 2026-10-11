package EdDYON.guaniao.client.guide;

import com.google.gson.Gson;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** Versioned, offline content. Web markup is converted at authoring time, never executed in game. */
record HandbookData(int schema, List<Page> pages, List<Category> categories) {
    static HandbookData read(Reader reader) {
        HandbookData data = new Gson().fromJson(reader, HandbookData.class);
        if (data == null || data.schema != 1 || data.pages == null || data.categories == null)
            throw new IllegalArgumentException("Invalid handbook data");
        List<Page> pages = new ArrayList<>();
        for (Page page : data.pages) pages.add(validatePage(page));
        for (Category category : data.categories) {
            require(category != null && category.id != null && category.title != null, "category");
        }
        return new HandbookData(data.schema, List.copyOf(pages), List.copyOf(data.categories));
    }

    private static Page validatePage(Page page) {
        require(page != null && page.id != null && !page.id.isBlank()
                && page.kind != null && page.title != null, "page identity/title");
        if (page.kind.equals("bird")) {
            require(page.model != null && !page.model.isBlank()
                    && ResourceLocation.tryBuild("guaniao", page.model) != null, "bird model");
        }
        String icon = optional(page.icon);
        if (page.kind.equals("item") || page.catalog && !page.kind.equals("bird")) {
            require(icon.isEmpty() || ResourceLocation.tryParse(icon) != null, "item icon");
        }
        List<Fact> facts = page.facts == null ? List.of() : page.facts;
        for (Fact fact : facts) require(fact != null && fact.label != null && fact.text != null, "fact");
        List<Section> sections = new ArrayList<>();
        if (page.sections != null) for (Section section : page.sections) {
            require(section != null && section.id != null && section.title != null
                    && section.blocks != null, "section blocks");
            List<Block> blocks = new ArrayList<>();
            for (Block block : section.blocks) blocks.add(validateBlock(block));
            sections.add(new Section(section.id, section.title, List.copyOf(blocks)));
        }
        return new Page(page.id, optional(page.route), page.kind, optional(page.category),
                optional(page.model), icon, page.catalog, page.title, optional(page.summary),
                List.copyOf(facts), List.copyOf(sections));
    }

    private static Block validateBlock(Block block) {
        require(block != null && block.type != null, "block type");
        boolean nonText = switch (block.type) {
            case "anchor", "recipe", "anvil", "sound" -> true;
            default -> false;
        };
        require(nonText || block.spans != null, "text block spans");
        List<Span> spans = new ArrayList<>();
        if (block.spans != null) for (Span span : block.spans) {
            require(span != null && span.text != null, "text span");
            spans.add(new Span(span.text, optional(span.link)));
        }
        switch (block.type) {
            case "recipe" -> requireResource(block.recipe, "recipe");
            case "sound" -> requireResource(block.sound, "sound");
            case "anvil" -> require(List.of("guaniao:burial_plume_book", "guaniao:riven_plume_book",
                    "guaniao:hunting_return_book").contains(optional(block.book)), "anvil book");
        }
        return new Block(block.type, optional(block.id), List.copyOf(spans),
                block.recipe, block.sound, block.book);
    }

    private static void requireResource(String id, String field) {
        require(id != null && !id.isBlank() && ResourceLocation.tryParse(id) != null, field);
    }

    private static String optional(String value) { return value == null ? "" : value; }

    private static void require(boolean valid, String field) {
        if (!valid) throw new IllegalArgumentException("Invalid handbook " + field);
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
