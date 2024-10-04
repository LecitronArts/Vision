package me.empty.api.event;

public abstract class Cancelable {
    private boolean cancel;

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public void cancelEvent() {
        this.cancel = true;
    }
}
