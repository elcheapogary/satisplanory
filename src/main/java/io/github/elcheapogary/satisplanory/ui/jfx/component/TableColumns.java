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

import java.util.Comparator;
import java.util.function.Function;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class TableColumns
{
    private TableColumns()
    {
    }

    public static <R, D> TableColumn<R, D> createNumericColumn(String name, Function<? super R, ? extends D> dataExtractor, Function<? super D, String> toStringConverter, Comparator<D> comparator)
    {
        TableColumn<R, D> col = new TableColumn<>(name);
        col.setComparator(new NullsLastComparator<>(col.sortTypeProperty(), comparator));
        col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(dataExtractor.apply(param.getValue())));
        col.setCellFactory(param -> {
            TableCell<R, D> cell = new TableCell<>()
            {
                @Override
                protected void updateItem(D item, boolean empty)
                {
                    if (item == null || empty){
                        setText("");
                    }else{
                        setText(toStringConverter.apply(item));
                    }
                }
            };

            cell.setAlignment(Pos.BASELINE_RIGHT);

            return cell;
        });

        return col;
    }

    private static class NullsLastComparator<T>
            implements Comparator<T>
    {
        private final ObservableValue<TableColumn.SortType> sortType;
        private final Comparator<T> comparator;

        public NullsLastComparator(ObservableValue<TableColumn.SortType> sortType, Comparator<T> comparator)
        {
            this.sortType = sortType;
            this.comparator = comparator;
        }

        @Override
        public int compare(T o1, T o2)
        {
            if (o1 == null){
                if (o2 == null){
                    return 0;
                }else if (sortType.getValue() == TableColumn.SortType.ASCENDING){
                    return 1;
                }else if (sortType.getValue() == TableColumn.SortType.DESCENDING){
                    return -1;
                }else{
                    return 0;
                }
            }else if (o2 == null){
                if (sortType.getValue() == TableColumn.SortType.ASCENDING){
                    return -1;
                }else if (sortType.getValue() == TableColumn.SortType.DESCENDING){
                    return 1;
                }else{
                    return 0;
                }
            }else{
                return comparator.compare(o1, o2);
            }
        }

        @Override
        public Comparator<T> reversed()
        {
            return new NullsLastComparator<>(sortType, comparator.reversed());
        }
    }
}
