/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.component;

import java.math.BigDecimal;
import java.util.function.Consumer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;

public class BigDecimalTextField
{
    private BigDecimalTextField()
    {
    }

    public static void setUp(TextField textField, BigDecimal initialValue, Consumer<? super BigDecimal> onChange)
    {
        textField.setText(initialValue.toString());
        textField.setAlignment(Pos.BASELINE_RIGHT);
        textField.focusedProperty().addListener(new ChangeListener<>()
        {
            private String focusText;

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                if (newValue){
                    focusText = textField.getText();
                }else{
                    String text = textField.getText().trim();
                    if (!text.matches("^\\d+(\\.\\d+)?$")){
                        textField.setText(focusText);
                    }else{
                        onChange.accept(new BigDecimal(text));
                    }
                }
            }
        });
    }
}
