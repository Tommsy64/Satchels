package io.github.tommsy64.satchels;

import lombok.NonNull;

public enum Keys {
    IS_BACKPACK("isBackpack"), TITLE("backpack-name"), CONTENTS("backpack-contents"), ROWS("backpack-size"), UUID("backpack-uuid");

    public final String key;

    private Keys(@NonNull String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
