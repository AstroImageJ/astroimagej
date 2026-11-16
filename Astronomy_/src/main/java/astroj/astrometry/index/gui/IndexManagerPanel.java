package astroj.astrometry.index.gui;

import astroj.astrometry.index.AstrometryNetIndexManager;
import astroj.astrometry.index.IndexOptions.CustomLocation;
import astroj.astrometry.index.data.IndexCatalog;
import astroj.astrometry.index.data.IndexSeries;
import ij.IJ;
import ij.astro.util.UIHelper;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class IndexManagerPanel extends JPanel {
    private final AstrometryNetIndexManager manager;
    private final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final JTree tree = new JTree(treeModel);
    private final JTextField indexRootField = new JTextField();
    private final JLabel rangeLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");
    private final JProgressBar fileProgress = new JProgressBar(0, 100);
    private final JProgressBar overallProgress = new JProgressBar(0, 100);
    private final JPanel progressPanel = new JPanel(new GridLayout(2, 1, 0, 4));

    public IndexManagerPanel(AstrometryNetIndexManager manager) {
        super(new BorderLayout(8, 8));
        UIHelper.setLookAndFeel();
        this.manager = manager;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tree.setRootVisible(false);
        tree.setRowHeight(0);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.setCellRenderer(new CheckRenderer());
        tree.addTreeSelectionListener(_ -> updateRange());
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleAt(e);
            }
        });
        ToolTipManager.sharedInstance().registerComponent(tree);

        var center = new JPanel(new BorderLayout(4, 4));
        center.add(buildRootRow(), BorderLayout.NORTH);
        center.add(new JScrollPane(tree), BorderLayout.CENTER);
        center.add(rangeLabel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        indexRootField.setText(manager.options().indexRoot().toString());
        statusLabel.setText("Loading indexes...");
        // The manager does no scanning or fetching until the GUI asks, refresh once it finishes
        manager.loadForGui().whenComplete((_, _) -> SwingUtilities.invokeLater(() -> {
            refresh();
            if (manager.lastFetchError() != null) {
                statusLabel.setText("Could not fetch remote index list");
            } else {
                statusLabel.setText(" ");
            }
        }));
    }

    static void main() {
        var manager = new AstrometryNetIndexManager();
        SwingUtilities.invokeLater(() -> {
            var panel = new IndexManagerPanel(manager);
            var frame = new JFrame("Index Manager");
            frame.add(panel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    private JPanel buildRootRow() {
        var rootRow = new JPanel(new BorderLayout(4, 0));
        rootRow.add(new JLabel("Download folder:"), BorderLayout.WEST);
        indexRootField.setEditable(false);
        rootRow.add(indexRootField, BorderLayout.CENTER);
        var browse = new JButton("Browse...");
        browse.addActionListener(_ -> chooseDirectory(indexRootField.getText()).ifPresent(p -> {
            manager.options().setIndexRoot(p);
            manager.refreshLocal();
            refresh();
        }));
        rootRow.add(browse, BorderLayout.EAST);
        return rootRow;
    }

    private JPanel buildButtons() {
        var panel = new JPanel(new BorderLayout());
        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        var addCustom = new JButton("Add custom...");
        addCustom.setToolTipText("Add a folder of index files under an id. Its contents are used as is.");
        addCustom.addActionListener(_ -> addCustomLocation());
        var removeCustom = new JButton("Remove custom");
        removeCustom.addActionListener(_ -> removeSelectedCustom());
        var download = new JButton("Download selected");
        download.addActionListener(_ -> downloadSelected(download));
        var save = new JButton("Save");
        save.addActionListener(_ -> {
            try {
                manager.saveOptions();
                statusLabel.setText("Saved");
            } catch (IOException ex) {
                statusLabel.setText("Save failed: " + ex.getMessage());
            }
        });

        buttons.add(addCustom);
        buttons.add(removeCustom);
        buttons.add(download);
        buttons.add(save);
        fileProgress.setStringPainted(true);
        overallProgress.setStringPainted(true);
        progressPanel.add(fileProgress);
        progressPanel.add(overallProgress);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        progressPanel.setVisible(false);

        panel.add(progressPanel, BorderLayout.NORTH);
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    /// Rebuilds the tree from the manager, keeping expanded series open.
    public void refresh() {
        var expanded = new HashSet<String>();
        var root = (DefaultMutableTreeNode) treeModel.getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            var node = (DefaultMutableTreeNode) root.getChildAt(i);
            var path = new TreePath(node.getPath());
            if (node.getUserObject() instanceof SeriesData(IndexSeries series) && tree.isExpanded(path)) {
                expanded.add(series.id());
            } else if (node.getUserObject() instanceof CatalogGroup && tree.isExpanded(path)) {
                // The catalog folder name is reserved, so it cannot clash with a series id
                expanded.add(AstrometryNetIndexManager.CATALOG_DIRECTORY);
            }
        }

        root.removeAllChildren();
        for (var s : manager.series()) {
            var seriesNode = new DefaultMutableTreeNode(new SeriesData(s));
            s.families().stream()
                    .sorted(Comparator.comparingDouble(IndexSeries.IndexFamily::scaleLower)
                            .thenComparing(IndexSeries.IndexFamily::scaleUpper))
                    .forEach(f -> seriesNode.add(new DefaultMutableTreeNode(new FamilyData(s, f), false)));
            root.add(seriesNode);
        }

        var catalogs = manager.catalogs();
        if (!catalogs.isEmpty()) {
            var group = new DefaultMutableTreeNode(new CatalogGroup());
            catalogs.forEach(c -> group.add(new DefaultMutableTreeNode(new CatalogData(c), false)));
            root.add(group);
        }

        for (var custom : manager.options().customLocations()) {
            root.add(new DefaultMutableTreeNode(new CustomData(custom), false));
        }

        treeModel.reload();
        for (int i = 0; i < root.getChildCount(); i++) {
            var node = (DefaultMutableTreeNode) root.getChildAt(i);
            if (node.getUserObject() instanceof SeriesData(IndexSeries series) && expanded.contains(series.id())) {
                tree.expandPath(new TreePath(node.getPath()));
            } else if (node.getUserObject() instanceof CatalogGroup
                    && expanded.contains(AstrometryNetIndexManager.CATALOG_DIRECTORY)) {
                tree.expandPath(new TreePath(node.getPath()));
            }
        }

        indexRootField.setText(manager.options().indexRoot().toString());
        updateRange();
    }

    private void toggleAt(MouseEvent e) {
        var path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }

        // Only the checkbox area toggles, so row clicks stay available for range selection
        var bounds = tree.getPathBounds(path);
        if (bounds == null || e.getX() > bounds.x + new JCheckBox().getPreferredSize().width) {
            return;
        }

        var node = (DefaultMutableTreeNode) path.getLastPathComponent();
        var options = manager.options();
        if (node.getUserObject() instanceof FamilyData(IndexSeries series1, IndexSeries.IndexFamily family)) {
            if (!manager.canToggleFamily(series1, family)) {
                statusLabel.setText("Downloaded families follow their series, disable the series instead");
                return;
            }

            treeModel.nodeChanged(node);
        } else if (node.getUserObject() instanceof SeriesData(IndexSeries series)) {
            options.setSeriesEnabled(series.id(), !options.isSeriesEnabled(series.id()));
            treeModel.nodeChanged(node);
            for (int i = 0; i < node.getChildCount(); i++) {
                treeModel.nodeChanged(node.getChildAt(i));
            }
        } else if (node.getUserObject() instanceof CustomData(CustomLocation location)) {
            options.setSeriesEnabled(location.id(), !options.isSeriesEnabled(location.id()));
            treeModel.nodeChanged(node);
        }
    }

    private void addCustomLocation() {
        var dir = chooseDirectory(null);
        if (dir.isEmpty()) {
            return;
        }

        var suggestion = dir.get().getFileName() == null ? "" : dir.get().getFileName().toString();
        while (true) {
            var id = (String) JOptionPane.showInputDialog(this,
                    "Id for this location:",
                    "Add custom index location", JOptionPane.PLAIN_MESSAGE, null, null, suggestion);
            if (id == null) {
                return;
            }

            id = id.trim();
            if (manager.isIdAvailable(id) && manager.options().addCustomLocation(id, dir.get())) {
                refresh();
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "The id must be non-empty, contain no '/', and not match " + "an existing series or custom location.",
                    "Invalid id", JOptionPane.WARNING_MESSAGE);
            suggestion = id;
        }
    }

    private void removeSelectedCustom() {
        var paths = tree.getSelectionPaths();
        if (paths == null) {
            return;
        }

        var ids = new ArrayList<String>();
        for (var p : paths) {
            if (((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject() instanceof CustomData(
                    CustomLocation location
            )) {
                ids.add(location.id());
            }
        }

        ids.forEach(manager.options()::removeCustomLocation);
        if (!ids.isEmpty()) {
            manager.refreshLocal();
            refresh();
        }
    }

    private List<FamilyData> selectedFamilies() {
        var out = new ArrayList<FamilyData>();
        var paths = tree.getSelectionPaths();
        if (paths == null) {
            return out;
        }

        for (var p : paths) {
            var node = (DefaultMutableTreeNode) p.getLastPathComponent();
            if (node.getUserObject() instanceof FamilyData fd) {
                out.add(fd);
            } else if (node.getUserObject() instanceof SeriesData(IndexSeries series)) {
                series.families().forEach(f -> out.add(new FamilyData(series, f)));
            }
        }

        return out;
    }

    /// Selecting the Catalogs group selects every catalog.
    private List<IndexCatalog> selectedCatalogs() {
        var paths = tree.getSelectionPaths();
        if (paths == null) {
            return List.of();
        }

        var out = new LinkedHashSet<IndexCatalog>();
        for (var p : paths) {
            var node = (DefaultMutableTreeNode) p.getLastPathComponent();
            if (node.getUserObject() instanceof CatalogData(IndexCatalog catalog)) {
                out.add(catalog);
            } else if (node.getUserObject() instanceof CatalogGroup) {
                out.addAll(manager.catalogs());
            }
        }

        return List.copyOf(out);
    }

    private void updateRange() {
        var selected = selectedFamilies();
        var range = scaleRange(selected.stream().map(FamilyData::key).toList());
        if (range.isEmpty()) {
            rangeLabel.setText(" ");
            return;
        }

        rangeLabel.setText("Selected range: " +
                IndexSeries.IndexFamily.formatScale(range.get().lower()) + " to " +
                IndexSeries.IndexFamily.formatScale(range.get().upper()) +
                " (" + selected.size() + (selected.size() == 1 ? " family)" : " families)"));
    }

    /// Selecting multiple families is treated as a range spanning all of them.
    private Optional<ScaleRange> scaleRange(Collection<FamilyKey> keys) {
        double lower = Double.POSITIVE_INFINITY;
        double upper = Double.NEGATIVE_INFINITY;
        for (var key : keys) {
            var family = find(key);
            if (family == null) {
                continue;
            }

            lower = Math.min(lower, family.scaleLower());
            upper = Math.max(upper, family.scaleUpper());
        }

        return lower <= upper ? Optional.of(new ScaleRange(lower, upper)) : Optional.empty();
    }

    private IndexSeries.IndexFamily find(FamilyKey key) {
        for (var s : manager.series()) {
            if (!s.id().equals(key.seriesId())) {
                continue;
            }

            for (var family : s.families()) {
                if (family.id() == key.familyId()) {
                    return family;
                }
            }
        }

        return null;
    }

    private void downloadSelected(JButton trigger) {
        var selected = selectedFamilies().stream().filter(fd -> !manager.isInstalled(fd.series(), fd.family())).toList();
        var selectedCatalogs = selectedCatalogs().stream().filter(c -> !manager.isInstalled(c)).toList();
        if (selected.isEmpty() && selectedCatalogs.isEmpty()) {
            IJ.error("Select one or more families or catalogs that are not downloaded");
            return;
        }

        var totalFiles = selected.stream().mapToInt(fd -> downloadableFiles(fd.family().files())).sum() +
                selectedCatalogs.stream().mapToInt(c -> downloadableFiles(c.files())).sum();

        trigger.setEnabled(false);
        resetProgress(totalFiles);
        progressPanel.setVisible(true);
        new SwingWorker<Void, DownloadProgress>() {
            private int filesDone = 0;

            @Override
            protected Void doInBackground() throws IOException {
                var listener = new AstrometryNetIndexManager.DownloadListener() {
                    @Override
                    public void fileStarted(IndexSeries.IndexFamily.IndexFile file) {
                        publish(new DownloadProgress(file.name(), 0, -1, filesDone, totalFiles));
                    }

                    @Override
                    public void fileProgress(IndexSeries.IndexFamily.IndexFile file, long bytesRead, long total) {
                        publish(new DownloadProgress(file.name(), bytesRead, total, filesDone, totalFiles));
                    }

                    @Override
                    public void fileFinished(IndexSeries.IndexFamily.IndexFile file) {
                        filesDone++;
                        publish(new DownloadProgress(file.name(), 1, 1, filesDone, totalFiles));
                    }
                };

                for (var fd : selected) {
                    manager.download(fd.series(), fd.family(), listener);
                }

                for (var catalog : selectedCatalogs) {
                    manager.downloadCatalog(catalog, listener);
                }

                return null;
            }

            @Override
            protected void process(List<DownloadProgress> chunks) {
                applyProgress(chunks.getLast());
            }

            @Override
            protected void done() {
                trigger.setEnabled(true);
                progressPanel.setVisible(false);
                try {
                    get();
                    statusLabel.setText("Download complete");
                } catch (Exception ex) {
                    var cause = ex.getCause() == null ? ex : ex.getCause();
                    statusLabel.setText("Download failed: " + cause.getMessage());
                }

                refresh();
            }
        }.execute();
    }

    /// Files the manager will actually process, it skips entries without a hash or source.
    private static int downloadableFiles(List<IndexSeries.IndexFamily.IndexFile> files) {
        return (int) files.stream().filter(f -> !f.sha256().isEmpty() && f.source() != null).count();
    }

    private void resetProgress(int totalFiles) {
        fileProgress.setIndeterminate(false);
        fileProgress.setValue(0);
        fileProgress.setString(" ");
        overallProgress.setMaximum(Math.max(totalFiles, 1));
        overallProgress.setValue(0);
        overallProgress.setString("0 / " + totalFiles + " files");
    }

    private void applyProgress(DownloadProgress p) {
        if (p.total() > 0) {
            fileProgress.setIndeterminate(false);
            fileProgress.setValue((int) Math.min(100, 100 * p.bytesRead() / p.total()));
            fileProgress.setString(p.fileName() + "  " + formatBytes(p.bytesRead()) + " / " + formatBytes(p.total()));
        } else {
            fileProgress.setIndeterminate(true);
            fileProgress.setString("Checking existing " + p.fileName() + "...");
        }

        overallProgress.setValue(p.filesDone());
        overallProgress.setString(p.filesDone() + " / " + p.fileCount() + " files");
        statusLabel.setText("Downloading " + p.fileName());
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        var units = new String[]{"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);

        return "%.1f %s".formatted(value, units[unit]);
    }

    private Optional<Path> chooseDirectory(String start) {
        var chooser = new JFileChooser(start == null || start.isBlank() ? null : start);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return Optional.of(chooser.getSelectedFile().toPath());
        }

        return Optional.empty();
    }

    /// Scale coverage (radians) of a group of families.
    private record ScaleRange(double lower, double upper) {
    }

    private record SeriesData(IndexSeries series) {
        @Override
        public String toString() {
            return series.name();
        }
    }

    private record FamilyData(IndexSeries series, IndexSeries.IndexFamily family) {
        FamilyKey key() {
            return new FamilyKey(series.id(), family.id());
        }

        @Override
        public String toString() {
            return family.toString();
        }
    }

    private record CustomData(CustomLocation location) {
        @Override
        public String toString() {
            return location.id();
        }
    }

    /// The "Catalogs" parent node.
    private record CatalogGroup() {
        @Override
        public String toString() {
            return "Catalogs";
        }
    }

    private record CatalogData(IndexCatalog catalog) {
        @Override
        public String toString() {
            return catalog.name();
        }
    }

    private record FamilyKey(String seriesId, int familyId) {
    }

    private record DownloadProgress(String fileName, long bytesRead, long total, int filesDone, int fileCount) {
    }

    private class CheckRenderer implements TreeCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        private final JCheckBox box = new JCheckBox();
        private final DefaultTreeCellRenderer label = new DefaultTreeCellRenderer();

        CheckRenderer() {
            panel.setOpaque(false);
            box.setOpaque(false);
            panel.add(box);
            panel.add(label);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            var node = (DefaultMutableTreeNode) value;
            var options = manager.options();
            var text = String.valueOf(node.getUserObject());
            String tip = null;
            var dimmed = false;
            box.setEnabled(true);
            box.setVisible(true);

            if (node.getUserObject() instanceof FamilyData(IndexSeries series, IndexSeries.IndexFamily family)) {
                var installed = manager.isInstalled(series, family);
                box.setSelected(manager.isFamilyEnabled(series, family));
                // Downloaded families follow their series, so the box is locked
                box.setEnabled(!installed);
                // Don't show box for families as they are not always included
                box.setVisible(false);
                dimmed = !options.isSeriesEnabled(series.id());
                var size = formatBytes(family.totalSize());
                text += "  (" + size + ")" + (installed ? "  [downloaded]" : "  [not downloaded]");
                tip = family.files().size() + " file(s), " + size + (installed ? ". Follows its series." : " to download.");
            } else if (node.getUserObject() instanceof SeriesData(IndexSeries series)) {
                box.setSelected(options.isSeriesEnabled(series.id()));
                //tip = series.description();
                text = "<html><b>%s</b> %s</html>".formatted(series.name(), series.description());
            } else if (node.getUserObject() instanceof CatalogGroup) {
                // Catalogs have no enable state, they are only downloaded
                box.setVisible(false);
                text = "<html><b>Catalogs</b> Used for annotating plate solved images.</html>";
            } else if (node.getUserObject() instanceof CatalogData(IndexCatalog catalog)) {
                box.setVisible(false);
                var installed = manager.isInstalled(catalog);
                var size = formatBytes(catalog.totalSize());
                text = catalog.name() + "  (" + size + ")" + (installed ? "  [downloaded]" : "  [not downloaded]");
                tip = catalog.description();
            } else if (node.getUserObject() instanceof CustomData(CustomLocation location)) {
                box.setSelected(options.isSeriesEnabled(location.id()));
                text += "  [custom] " + location.path();
                tip = location.path().toString();
            }

            panel.setToolTipText(tip);
            label.getTreeCellRendererComponent(tree, text, selected, expanded, true, row, hasFocus);
            label.setIcon(null);
            label.setEnabled(!dimmed);
            return panel;
        }
    }
}