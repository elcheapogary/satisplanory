/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.data;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class AppData
{
    private final ObjectProperty<PersistentData> persistentData = new SimpleObjectProperty<>();
    private final ObjectProperty<GameData> gameData = new SimpleObjectProperty<>();

    public ObjectProperty<GameData> gameDataProperty()
    {
        return gameData;
    }

    public GameData getGameData()
    {
        return gameData.get();
    }

    public void setGameData(GameData gameData)
    {
        this.gameData.set(gameData);
    }

    public PersistentData getPersistentData()
    {
        return persistentData.get();
    }

    public void setPersistentData(PersistentData persistentData)
    {
        this.persistentData.set(persistentData);
    }

    public ObjectProperty<PersistentData> persistentDataProperty()
    {
        return persistentData;
    }
}
