/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.component;

import io.github.elcheapogary.satisplanory.gamedata.Item;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;

public class ItemComponents
{
    private ItemComponents()
    {
    }

    public static ComboBox<Item> createItemComboBox(ObservableList<Item> items)
    {
        ComboBox<Item> cb = new SearchableComboBox<>();
        cb.setConverter(new StringConverter<>()
        {
            @Override
            public Item fromString(String string)
            {
                return null;
            }

            @Override
            public String toString(Item item)
            {
                if (item == null){
                    return null;
                }

                return item.getName();
            }
        });
        cb.setItems(items);
        return cb;
    }
}
