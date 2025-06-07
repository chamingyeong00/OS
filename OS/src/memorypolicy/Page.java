package memorypolicy;

public class Page {
    public static int CREATE_ID = 0;
//    고유한 pid를 부여하기 위한 정적 변수

    public enum STATUS {
        HIT,
        PAGEFAULT,
        MIGRATION
    }

    public int pid;
    public int loc;
    public char data;
    public STATUS status;

    public Page() {}

    public Page(int pid, int loc, char data, STATUS status) {
        this.pid = pid;
        this.loc = loc;
        this.data = data;
        this.status = status;
    }
}
