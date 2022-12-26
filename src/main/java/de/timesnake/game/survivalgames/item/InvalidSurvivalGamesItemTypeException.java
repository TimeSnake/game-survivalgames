/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.game.survivalgames.item;

public class InvalidSurvivalGamesItemTypeException extends Exception {

    private final String type;

    public InvalidSurvivalGamesItemTypeException(String type) {
        this.type = type;
    }

    @Override
    public String getMessage() {
        return "Invalid hunger games item type: " + this.type;
    }
}
