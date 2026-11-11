package dev.shared.milaaan.task.profit_stats.gui;

import dev.shared.milaaan.config.ProfitStatsConfig;
import dev.shared.milaaan.task.profit_stats.ProfitStats;
import dev.shared.milaaan.task.profit_stats.data.MapRecord;
import dev.shared.milaaan.task.profit_stats.rank.Calculator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Window opened from the extra menu: map ranking for the current equipment, NPC values and real map gains. */
public final class ProfitWindow extends JFrame {
    private static final int REFRESH_MS = 2_000;
    private static ProfitWindow instance;

    private final ProfitStats feature;
    private final Texts texts;
    private final JLabel header = new JLabel();
    private final RankingTableModel rankingModel;
    private final NpcTableModel npcModel;
    private final MapTableModel mapModel;
    private final Timer refresher;

    /** Must be called on the Swing thread. */
    public static void open(ProfitStats feature) {
        if (instance == null || instance.feature != feature) {
            if (instance != null) instance.dispose();
            instance = new ProfitWindow(feature);
        }
        instance.refresh();
        instance.setVisible(true);
        instance.toFront();
    }

    private ProfitWindow(ProfitStats feature) {
        super("Profit Stats");
        this.feature = feature;
        this.texts = feature.getTexts();
        this.rankingModel = new RankingTableModel(this.texts);
        this.npcModel = new NpcTableModel(this.texts);
        this.mapModel = new MapTableModel(this.texts);
        this.refresher = new Timer(REFRESH_MS, e -> this.refresh());

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 420));
        this.header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(this.header, BorderLayout.NORTH);
        getContentPane().add(this.buildTabs(), BorderLayout.CENTER);
        getContentPane().add(this.buildButtons(), BorderLayout.SOUTH);
        pack();
        setSize(1250, 600);
        setLocationRelativeTo(null);
        addWindowListener(new RefreshOnShow());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(this.texts.get("tab.ranking"), new JScrollPane(table(this.rankingModel, RankingTableModel.COL_URI_H)));
        tabs.addTab(this.texts.get("tab.npcs"), new JScrollPane(table(this.npcModel, NpcTableModel.COL_URI_H)));
        tabs.addTab(this.texts.get("tab.maps"), new JScrollPane(table(this.mapModel, MapTableModel.COL_URI_H)));
        return tabs;
    }

    private JPanel buildButtons() {
        JButton refresh = new JButton(this.texts.get("refresh"));
        refresh.addActionListener(e -> this.refresh());
        JButton reset = new JButton(this.texts.get("reset"));
        reset.addActionListener(e -> this.askReset());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refresh);
        buttons.add(reset);
        return buttons;
    }

    private static JTable table(AbstractTableModel model, int defaultSortColumn) {
        JTable table = new JTable(model);
        TableRowSorter<AbstractTableModel> sorter = new TableRowSorter<>(model);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(defaultSortColumn, SortOrder.DESCENDING)));
        table.setRowSorter(sorter);
        table.setDefaultRenderer(Double.class, new NumberRenderer());
        table.setDefaultRenderer(Integer.class, new NumberRenderer());
        table.setFillsViewportHeight(true);
        return table;
    }

    private void askReset() {
        int answer = JOptionPane.showConfirmDialog(this, this.texts.get("reset.confirm"),
                "Profit Stats", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        this.feature.getStore().reset();
        this.feature.getStore().saveIfDirty();
        this.refresh();
    }

    private void refresh() {
        ProfitStatsConfig config = this.feature.getConfig();
        String setup = this.feature.currentSetupKey();
        double dps = this.feature.currentDps();
        this.header.setText(this.headerText(config, setup, dps));

        List<MapRecord> mapRecords = this.feature.getStore().snapshotMaps();
        Calculator calculator = new Calculator(this.feature.getWiki(), this.feature.getStore().snapshotNpcs(), setup, dps);
        double minSeconds = config.minMapMinutes * 60.0;
        int company = config.ownCompanyOnly ? this.feature.currentCompany() : 0;

        this.rankingModel.setRows(calculator.rankMaps(mapRecords, config.overheadSeconds, config.onlyKillable, company, minSeconds));
        this.npcModel.update(calculator.allNpcs(), config.overheadSeconds);
        this.mapModel.setRows(measuredMaps(mapRecords, minSeconds));
    }

    private String headerText(ProfitStatsConfig config, String setup, double dps) {
        String dpsText;
        if (dps <= 0) dpsText = this.texts.get("dps.unknown");
        else if (this.feature.isManualDps()) dpsText = this.texts.get("dps.manual", NumberRenderer.format(dps));
        else dpsText = this.texts.get("dps.measured", NumberRenderer.format(dps), this.feature.currentDpsSamples());
        return "<html>" + this.texts.get("header", escape(setup), dpsText, config.overheadSeconds)
                + "<br><small>" + this.texts.get("header.help") + "</small></html>";
    }

    private static List<MapRecord> measuredMaps(List<MapRecord> mapRecords, double minSeconds) {
        List<MapRecord> measured = new ArrayList<>();
        for (MapRecord m : mapRecords) if (m.seconds >= minSeconds) measured.add(m);
        return measured;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Refreshes the tables every few seconds while the window is shown. */
    private final class RefreshOnShow extends WindowAdapter {
        @Override
        public void windowOpened(WindowEvent e) {
            ProfitWindow.this.refresher.start();
        }

        @Override
        public void windowActivated(WindowEvent e) {
            ProfitWindow.this.refresher.start();
        }

        @Override
        public void windowClosing(WindowEvent e) {
            ProfitWindow.this.refresher.stop();
            ProfitWindow.this.feature.getStore().saveIfDirty();
        }
    }
}
