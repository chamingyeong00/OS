package memorypolicy;

import java.util.*;

public class LfuCore implements CorePolicy {

    private final int capacity;
    private int time;  // 현재 시간 (접근 순서)
    private int hitCount;
    private int faultCount;
    private int migrationCount;
    private final Set<Integer> usedFrames = new HashSet<>();

    private final Map<Character, Page> pageMap; // 데이터(키) -> Page
    private final List<Page> pageHistory;

    // LFU 캐시를 위한 보조 구조체들
    private final Map<Integer, LinkedHashSet<Character>> freqMap; // freq -> 해당 freq를 가진 키들의 순서 집합
    private final Map<Character, Integer> keyFreq; // key -> freq
    private int minFreq;

    public LfuCore(int capacity) {
        this.capacity = capacity;
        this.time = 0;
        this.hitCount = 0;
        this.faultCount = 0;
        this.migrationCount = 0;

        this.pageMap = new HashMap<>();
        this.pageHistory = new ArrayList<>();

        this.freqMap = new HashMap<>();
        this.keyFreq = new HashMap<>();
        this.minFreq = 0;
    }

    @Override
    public Page.STATUS operate(char data) {
        time++;
        System.out.println("=== OPERATE START ===");
        System.out.println("Accessing data: " + data);
        System.out.println("Current minFreq: " + minFreq);
        System.out.println("Current pageMap keys: " + pageMap.keySet());
        System.out.println("Current freqMap keys: " + freqMap.keySet());

        // 1) 페이지가 이미 메모리에 있는 경우 (HIT)
        if (pageMap.containsKey(data)) {
            hitCount++;
            System.out.println("Page HIT: " + data);
            updateFreq(data);
            Page updatedPage = pageMap.get(data);
            System.out.printf("Updated Page info - pid: %d, loc: %d, data: %c\n",
                    updatedPage.pid, updatedPage.loc, data);
            Page p = new Page(updatedPage.pid, updatedPage.loc, data, Page.STATUS.HIT);
            pageHistory.add(p);
            System.out.println("Page added to history as HIT");
            System.out.println("=== OPERATE END ===\n");
            return Page.STATUS.HIT;
        }

        // 2) 페이지가 없는 경우 (MISS)
        faultCount++;
        System.out.println("Page MISS: " + data);

        // 3) 메모리 공간이 가득 찼으면 LFU 페이지 교체
        if (pageMap.size() >= capacity) {
            migrationCount++;
            System.out.println("Cache full, need migration");

            LinkedHashSet<Character> keys = freqMap.get(minFreq);
            char evictKey = keys.iterator().next();
            keys.remove(evictKey);
            if (keys.isEmpty()) {
                freqMap.remove(minFreq);
            }
            System.out.println("Evicting page: " + evictKey);

            Page evictedPage = pageMap.remove(evictKey);
            keyFreq.remove(evictKey);
            usedFrames.remove(evictedPage.loc);  // 프레임 반환

            Page newPage = new Page(Page.CREATE_ID++, evictedPage.loc, data, Page.STATUS.MIGRATION);
            pageMap.put(data, newPage);
            keyFreq.put(data, 1);
            freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(data);
            usedFrames.add(evictedPage.loc);
            minFreq = 1;

            System.out.printf("Inserted new page at loc %d: data %c\n", evictedPage.loc, data);
            pageHistory.add(newPage);
            System.out.println("Page added to history as MIGRATION");
            System.out.println("=== OPERATE END ===\n");
            return Page.STATUS.MIGRATION;
        }

        // 4) 공간이 남아있으면 새 페이지 삽입
        int loc = 0;
        for (; loc < capacity; loc++) {
            if (!usedFrames.contains(loc)) break;
        }

        Page newPage = new Page(Page.CREATE_ID++, loc, data, Page.STATUS.PAGEFAULT);
        pageMap.put(data, newPage);
        keyFreq.put(data, 1);
        freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(data);
        usedFrames.add(loc);
        minFreq = 1;

        System.out.printf("Inserted new page at loc %d: data %c\n", loc, data);
        pageHistory.add(newPage);
        System.out.println("Page added to history as PAGEFAULT");
        System.out.println("=== OPERATE END ===\n");
        return Page.STATUS.PAGEFAULT;
    }

    private void updateFreq(char key) {
        int freq = keyFreq.get(key);
        System.out.println("Updating freq for key: " + key + " from " + freq + " to " + (freq + 1));
        keyFreq.put(key, freq + 1);

        LinkedHashSet<Character> keys = freqMap.get(freq);
        keys.remove(key);
        if (keys.isEmpty()) {
            freqMap.remove(freq);
            if (freq == minFreq && !freqMap.containsKey(freq)) {
                minFreq = freq + 1;
                System.out.println("minFreq updated to: " + minFreq);
            }
        }
        freqMap.computeIfAbsent(freq + 1, k -> new LinkedHashSet<>()).add(key);
    }

    @Override
    public int getHitCount() {
        return hitCount;
    }

    @Override
    public int getFaultCount() {
        return faultCount;
    }

    @Override
    public int getMigrationCount() {
        return migrationCount;
    }

    @Override
    public List<Page> getPageHistory() {
        return pageHistory;
    }
}