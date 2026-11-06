package example.probe;

/** Also used by the separate, intentionally frame-stripping verifier experiment. */
public final class FrameProbe {
    public int select(boolean condition) {
        return condition ? 7 : 8;
    }
}
