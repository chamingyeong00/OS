package memorypolicy;

import java.util.*;

public class ScCore implements CorePolicy {
    private int p_frame_size;
    private int[] escPages;
    private boolean[] secondChance; // second chance bit
    private int pointer = 0;

    private List<Page> pageHistory;

    private int hit = 0;
    private int fault = 0;
    private int migration = 0;

    public ScCore(int frame_size) {
        this.p_frame_size = frame_size;
        escPages = new int[frame_size];
        Arrays.fill(escPages, -1);
        secondChance = new boolean[frame_size];
        pageHistory = new ArrayList<>();
    }

    @Override
    public Page.STATUS operate(char data) {
        Page newPage = new Page();
        newPage.pid = Page.CREATE_ID++;
        newPage.data = data;

        // HIT check
        for (int i = 0; i < p_frame_size; i++) {
            if (escPages[i] == data) {
                secondChance[i] = true;  // 두 번째 기회 부여
                newPage.status = Page.STATUS.HIT;
                hit++;
                newPage.loc = i + 1;
                pageHistory.add(newPage);
                return newPage.status;
            }
        }

        // MISS - page replacement needed
        while (true) {
            if (!secondChance[pointer]) {
                // 교체 대상 찾음
                escPages[pointer] = data;
                secondChance[pointer] = true;

                if (pageHistory.size() < p_frame_size) {
                    newPage.status = Page.STATUS.PAGEFAULT;
                    fault++;
                } else {
                    newPage.status = Page.STATUS.MIGRATION;
                    fault++;
                    migration++;
                }

                newPage.loc = pointer + 1;

                System.out.println("Replaced page at position " + pointer + " with data '" + data + "'");
                System.out.println("Current frame content: " + Arrays.toString(escPages));

                pointer = (pointer + 1) % p_frame_size;
                pageHistory.add(newPage);
                return newPage.status;
            }


            // 두 번째 기회로 bit 0으로 초기화 후 다음으로
            secondChance[pointer] = false;
            pointer = (pointer + 1) % p_frame_size;
        }

    }

    @Override
    public int getHitCount() {
        return hit;
    }

    @Override
    public int getFaultCount() {
        return fault;
    }

    @Override
    public int getMigrationCount() {
        return migration;
    }

    @Override
    public List<Page> getPageHistory() {
        return pageHistory;
    }
}