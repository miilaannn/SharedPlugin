package dev.shared.milaaan.task.profit_stats.gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Base for the read-only tables: fixed columns (translated keys) and a replaceable list of rows. */
abstract class ReadOnlyTableModel<T> extends AbstractTableModel {
    private final String[] columns;
    private final Class<?>[] types;
    protected List<T> rows = new ArrayList<>();

    ReadOnlyTableModel(Texts texts, String[] columnKeys, Class<?>[] types) {
        this.columns = new String[columnKeys.length];
        for (int i = 0; i < columnKeys.length; i++) this.columns[i] = texts.get("col." + columnKeys[i]);
        this.types = types;
    }

    void setRows(List<T> newRows) {
        this.rows = newRows;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return this.rows.size();
    }

    @Override
    public int getColumnCount() {
        return this.columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return this.columns[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return this.types[column];
    }
}
