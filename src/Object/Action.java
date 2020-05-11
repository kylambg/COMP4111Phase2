package Object;

public class Action {
    private int id;
    private String action;

    public Action(int id, String action) {
        this.id = id;
        this.action = action;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
