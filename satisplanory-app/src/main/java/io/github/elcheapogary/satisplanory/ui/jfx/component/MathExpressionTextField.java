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

import io.github.elcheapogary.satisplanory.util.MathExpression;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;

public class MathExpressionTextField
{
    private MathExpressionTextField()
    {
    }

    public static void setUp(TextField textField, MathExpression initialValue, Predicate<? super MathExpression> validator, Consumer<? super MathExpression> onChange)
    {
        textField.setText(initialValue.getExpression());
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
                    MathExpression expression;

                    try{
                        expression = MathExpression.parse(textField.getText().trim());
                    }catch (NumberFormatException e){
                        textField.setText(focusText);
                        return;
                    }

                    if (!validator.test(expression)){
                        textField.setText(focusText);
                        return;
                    }

                    onChange.accept(expression);
                }
            }
        });
    }
}
