package br.edu.ufape.taiti.gui.configuretask.fileview;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CheckBoxEditor extends DefaultCellEditor {

    private File file;

    public CheckBoxEditor(JCheckBox checkBox, File file) {
        super(checkBox);
        this.file = file;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        String fileLine = (String) table.getModel().getValueAt(row, 1);
        if (fileLine.strip().startsWith("Scenario") || (fileLine.strip().equals(file.getName()) && row == 0)) {
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        return null;
    }

}
