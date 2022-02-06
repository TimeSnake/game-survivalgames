package de.timesnake.game.hungergames.item;

public class InvalidHungerGamesItemTypeException extends Exception {

    private final String type;

    public InvalidHungerGamesItemTypeException(String type) {
        this.type = type;
    }

    @Override
    public String getMessage() {
        return "Invalid hunger games item type: " + this.type;
    }
}
