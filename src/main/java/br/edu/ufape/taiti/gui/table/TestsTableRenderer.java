package br.edu.ufape.taiti.gui.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static br.edu.ufape.taiti.gui.Constants.FIRST_ROW_HEIGHT;

public class TestsTableRenderer extends DefaultTableCellRenderer {

    private JCheckBox checkBox;

    public TestsTableRenderer() {
        this.checkBox = new JCheckBox();
        this.checkBox.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (column == 0) {
            Color bg;
            if (row == 0) {
                bg = table.getTableHeader().getBackground();
            } else {
                bg = isSelected ? table.getSelectionBackground() : table.getBackground();
            }
            checkBox.setBackground(bg);
            checkBox.setEnabled(true);
            checkBox.setVisible(true);
            checkBox.setSelected(value != null && (Boolean)value);

        } else {
            if (row == 0) {
                table.setRowHeight(0, FIRST_ROW_HEIGHT);
                c.setBackground(table.getTableHeader().getBackground());
            } else {
                Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
                c.setBackground(bg);
                TestRow t = ((TestsTableModel) table.getModel()).getRow(row);
                c.setToolTipText(t.getFile().getName());
            }
        }

        if (column == 0) {
            return this.checkBox;
        } else {
            return c;
        }

    }

}