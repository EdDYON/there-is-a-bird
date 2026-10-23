package EdDYON.guaniao.client.camera;

/** The shutter accepts a fresh press only after both mouse buttons have been released. */
final class CameraShutterInput {
    private boolean armed;
    private boolean resumeClickRequired;
    private boolean leftHeld;
    private boolean rightHeld;

    void reset() {
        armed = false;
        resumeClickRequired = false;
        leftHeld = false;
        rightHeld = false;
    }

    void suspend(boolean requireResumeClick) {
        armed = false;
        resumeClickRequired |= requireResumeClick;
    }

    void observe(boolean eligible, boolean leftDown, boolean rightDown) {
        if (!eligible) {
            armed = false;
            return;
        }
        if (!leftDown && !rightDown) {
            leftHeld = false;
            rightHeld = false;
            armed = !resumeClickRequired;
        }
    }

    boolean press(int button) {
        if (button == 0 ? leftHeld : rightHeld) {
            return false;
        }
        if (button == 0) {
            leftHeld = true;
        } else {
            rightHeld = true;
        }
        boolean accepted = armed && !resumeClickRequired;
        armed = false;
        resumeClickRequired = false;
        return accepted;
    }

    void release(int button) {
        if (button == 0) {
            leftHeld = false;
        } else if (button == 1) {
            rightHeld = false;
        }
    }

    void recaptured() {
        resumeClickRequired = false;
        armed = false;
    }

    boolean isArmed() {
        return armed;
    }
}
