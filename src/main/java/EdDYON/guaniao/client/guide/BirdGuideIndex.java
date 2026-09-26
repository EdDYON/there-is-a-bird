package EdDYON.guaniao.client.guide;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/** Search and paging keep the selected bird's original catalogue index stable. */
final class BirdGuideIndex {
    private List<Integer> matches = List.of();
    private int pageSize = 16;
    private int page;

    void search(int count, String query, IntFunction<String> searchableText) {
        String[] words = query.strip().toLowerCase(Locale.ROOT).split("\\s+");
        this.matches = IntStream.range(0, count).filter(index -> {
            String text = searchableText.apply(index).toLowerCase(Locale.ROOT);
            for (String word : words) {
                if (!text.contains(word)) return false;
            }
            return true;
        }).boxed().toList();
        this.page = 0;
    }

    void setPageSize(int pageSize) {
        int firstEntry = this.page * this.pageSize;
        this.pageSize = Math.max(1, pageSize);
        this.page = Math.min(firstEntry / this.pageSize, pageCount() - 1);
    }

    void movePage(int direction) {
        this.page = Math.max(0, Math.min(pageCount() - 1, this.page + direction));
    }

    int page() { return this.page; }
    int pageCount() { return Math.max(1, (this.matches.size() + this.pageSize - 1) / this.pageSize); }
    int count() { return this.matches.size(); }

    List<Integer> visible() {
        int start = this.page * this.pageSize;
        return this.matches.subList(start, Math.min(this.matches.size(), start + this.pageSize));
    }
}
