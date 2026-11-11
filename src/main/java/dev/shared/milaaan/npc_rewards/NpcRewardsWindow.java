package dev.shared.milaaan.npc_rewards;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Window listing every NPC with its maps, HP, rewards and rewards per 1,000 HP. Click a header to sort. */
final class NpcRewardsWindow extends JFrame {
    private static NpcRewardsWindow instance;

    private final TableRowSorter<AlienTableModel> sorter;
    private final JTextField search = new JTextField(20);

    /** Must be called on the Swing thread. */
    static void open(List<AlienEntry> aliens, Texts texts) {
        if (instance == null) instance = new NpcRewardsWindow(aliens, texts);
        instance.setVisible(true);
        instance.toFront();
    }

    private NpcRewardsWindow(List<AlienEntry> aliens, Texts texts) {
        super("NPC Rewards");
        AlienTableModel model = new AlienTableModel(aliens, texts);
        this.sorter = new TableRowSorter<>(model);
        this.sorter.setSortKeys(Collections.singletonList(
                new RowSorter.SortKey(AlienTableModel.COL_URI_PER_HP, SortOrder.DESCENDING)));

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 400));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(this.header(texts), BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(this.table(model)), BorderLayout.CENTER);
        setSize(1300, 650);
        setLocationRelativeTo(null);
    }

    private JPanel header(Texts texts) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(new JLabel(texts.get("search")), BorderLayout.WEST);
        searchPanel.add(this.search, BorderLayout.CENTER);
        this.search.getDocument().addDocumentListener(new SearchListener());
        panel.add(searchPanel, BorderLayout.WEST);
        panel.add(new JLabel("<html><small>" + texts.get("help") + "</small></html>"), BorderLayout.SOUTH);
        return panel;
    }

    private JTable table(AlienTableModel model) {
        JTable table = new JTable(model);
        table.setRowSorter(this.sorter);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Integer.class, new NumberRenderer(false));
        table.setDefaultRenderer(Double.class, new NumberRenderer(false));
        NumberRenderer ratios = new NumberRenderer(true);
        for (int c = AlienTableModel.FIRST_RATIO_COLUMN; c < model.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(ratios);
        }
        return table;
    }

    /** Filters rows on the NPC name and maps as the user types. */
    private final class SearchListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            this.apply();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            this.apply();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            this.apply();
        }

        private void apply() {
            String text = NpcRewardsWindow.this.search.getText().trim();
            NpcRewardsWindow.this.sorter.setRowFilter(text.isEmpty() ? null
                    : RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 2));
        }
    }
}
