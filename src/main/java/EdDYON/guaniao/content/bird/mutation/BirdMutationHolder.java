package EdDYON.guaniao.content.bird.mutation;

/** Implemented by bird entities that can carry a genetic mutation. */
public interface BirdMutationHolder {
    BirdMutation getBirdMutation();

    void setBirdMutation(BirdMutation mutation);
}
