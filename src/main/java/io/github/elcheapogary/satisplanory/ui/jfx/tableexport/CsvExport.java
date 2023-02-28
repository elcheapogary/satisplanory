/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.tableexport;

import java.io.IOException;
import java.io.Writer;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CsvExport
{
    private CsvExport()
    {
    }

    public static <S> void exportToCsv(Writer writer, TableView<S> table)
            throws IOException
    {
        {
            boolean first = true;
            for (TableColumn<S, ?> col : table.getColumns()){
                if (first){
                    first = false;
                }else{
                    writer.append(',');
                }
                writer.append('"');
                writer.append(col.getText().replace("\"", "\"\""));
                writer.append('"');
            }
        }

        for (S row : table.getItems()){
            writer.append("\r\n");
            boolean first = true;
            for (TableColumn<S, ?> col : table.getColumns()){
                if (first){
                    first = false;
                }else{
                    writer.append(',');
                }

                Object v = getColumnValue(table, col, row);

                if (v != null){
                    if (!(v instanceof Number)){
                        writer.append('"');
                    }
                    writer.append(v.toString().replace("\"", "\"\""));
                    if (!(v instanceof Number)){
                        writer.append('"');
                    }
                }
            }
        }
    }

    private static <S, T> T getColumnValue(TableView<S> table, TableColumn<S, T> column, S row)
    {
        return column.getCellValueFactory().call(new TableColumn.CellDataFeatures<>(table, column, row)).getValue();
    }
}
