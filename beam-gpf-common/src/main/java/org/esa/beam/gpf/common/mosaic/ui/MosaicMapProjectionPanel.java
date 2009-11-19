package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.gpf.common.reproject.ui.CrsSelectionPanel;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;

import javax.measure.unit.NonSI;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicMapProjectionPanel extends JPanel {

    private final AppContext appContext;
    private final MosaicFormModel mosaicModel;

    private CrsSelectionPanel crsSelectionPanel;
    private final BindingContext binding;
    private String[] demValueSet;
    private JLabel pixelXUnit;
    private JLabel pixelYUnit;
    private Map<String, Double> unitMap;
    private JFormattedTextField pixelSizeXField;
    private JFormattedTextField pixelSizeYField;

    MosaicMapProjectionPanel(AppContext appContext, MosaicFormModel mosaicModel) {
        this.appContext = appContext;
        this.mosaicModel = mosaicModel;
        binding = new BindingContext(mosaicModel.getPropertyContainer());
        unitMap = new HashMap<String, Double>();
        unitMap.put("°", 0.05);
        unitMap.put("m", 1000.0);
        unitMap.put("km", 1.0);
        init();
        createUI();
        updateForCrsChanged();
        binding.adjustComponents();

    }

    private void init() {
        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        if (demValueSet.length > 0) {
            mosaicModel.getPropertyContainer().setValue("elevationModelName", demValueSet[0]);
        }

        mosaicModel.getPropertyContainer().addPropertyChangeListener("updateMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Boolean updateMode = (Boolean) evt.getNewValue();
                Boolean enabled1 = !updateMode;
                crsSelectionPanel.setEnabled(enabled1);
            }
        });
    }

    private void createUI() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        setLayout(layout);
        crsSelectionPanel = new CrsSelectionPanel(appContext, false);
        crsSelectionPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateForCrsChanged();
            }
        });
        JPanel orthorectifyPanel = createOrthorectifyPanel();
        JPanel mosaicBoundsPanel = createMosaicBoundsPanel();
        add(crsSelectionPanel);
        add(orthorectifyPanel);
        add(mosaicBoundsPanel);
    }

    private void updateForCrsChanged() {
        final DirectPosition referencePos = mosaicModel.getGeoEnvelope().getMedian();
        final float lon = (float) referencePos.getOrdinate(0);
        final float lat = (float) referencePos.getOrdinate(1);
        try {
            final CoordinateReferenceSystem crs = crsSelectionPanel.getCrs(new GeoPos(lat, lon));
            if(crs != null){
                updatePixelUnit(crs);
                mosaicModel.setWkt(crs.toWKT());
            } else {
                mosaicModel.setWkt(null);
            }
        } catch (FactoryException ignored) {
            mosaicModel.setWkt(null);
        }
    }

    private void updatePixelUnit(CoordinateReferenceSystem crs) {
        final CoordinateSystem coordinateSystem = crs.getCoordinateSystem();
        final String unitX = coordinateSystem.getAxis(0).getUnit().toString();
        if (!unitX.equals(pixelXUnit.getText())) {
            pixelXUnit.setText(unitX);
            pixelSizeXField.setValue(unitMap.get(unitX));
        }
        final String unitY = coordinateSystem.getAxis(1).getUnit().toString();
        if (!unitY.equals(pixelYUnit.getText())) {
            pixelYUnit.setText(unitY);
            pixelSizeYField.setValue(unitMap.get(unitY));
        }
    }

    private JPanel createMosaicBoundsPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Mosaic Bounds"));
        final JPanel inputPanel = createBoundsInputPanel();
        panel.add(inputPanel);
        final WorldMapPaneDataModel worldMapModel = new WorldMapPaneDataModel();
        setMapBoundary(worldMapModel);
        final WorldMapPane worlMapPanel = new WorldMapPane(worldMapModel);
        final PropertyContainer propertyContainer = mosaicModel.getPropertyContainer();
        propertyContainer.addPropertyChangeListener(new MapBoundsChangeListener(worldMapModel));
        worlMapPanel.setMinimumSize(new Dimension(250, 125));
        worlMapPanel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(worlMapPanel);

        return panel;
    }

    private JPanel createBoundsInputPanel() {
        final TableLayout layout = new TableLayout(9);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setColumnWeightX(3, 0.0);
        layout.setColumnWeightX(4, 1.0);
        layout.setColumnWeightX(5, 0.0);
        layout.setColumnWeightX(6, 0.0);
        layout.setColumnWeightX(7, 1.0);
        layout.setColumnWeightX(8, 0.0);
        layout.setColumnPadding(2, new Insets(3, 0, 3, 12));
        layout.setColumnPadding(5, new Insets(3, 0, 3, 12));
        final JPanel panel = new JPanel(layout);
        final DoubleFormatter doubleFormatter = new DoubleFormatter("###0.0##");
        pixelXUnit = new JLabel(NonSI.DEGREE_ANGLE.toString());
        pixelYUnit = new JLabel(NonSI.DEGREE_ANGLE.toString());

        panel.add(new JLabel("West:"));
        final JFormattedTextField westLonField = new JFormattedTextField(doubleFormatter);
        westLonField.setHorizontalAlignment(JTextField.RIGHT);
        binding.bind("westBound", westLonField);
        binding.bindEnabledState("westBound", false, "updateMode", true);
        panel.add(westLonField);
        panel.add(new JLabel(NonSI.DEGREE_ANGLE.toString()));
        panel.add(new JLabel("East:"));
        final JFormattedTextField eastLonField = new JFormattedTextField(doubleFormatter);
        eastLonField.setHorizontalAlignment(JTextField.RIGHT);
        binding.bind("eastBound", eastLonField);
        binding.bindEnabledState("eastBound", false, "updateMode", true);
        panel.add(eastLonField);
        panel.add(new JLabel(NonSI.DEGREE_ANGLE.toString()));
        panel.add(new JLabel("Pixel size X:"));
        pixelSizeXField = new JFormattedTextField(doubleFormatter);
        pixelSizeXField.setHorizontalAlignment(JTextField.RIGHT);
        binding.bind("pixelSizeX", pixelSizeXField);
        binding.bindEnabledState("pixelSizeX", false, "updateMode", true);
        panel.add(pixelSizeXField);
        panel.add(pixelXUnit);
        
        panel.add(new JLabel("North:"));
        final JFormattedTextField northLatField = new JFormattedTextField(doubleFormatter);
        northLatField.setHorizontalAlignment(JTextField.RIGHT);
        binding.bind("northBound", northLatField);
        binding.bindEnabledState("northBound", false, "updateMode", true);
        panel.add(northLatField);
        panel.add(new JLabel(NonSI.DEGREE_ANGLE.toString()));
        panel.add(new JLabel("South:"));
        final JFormattedTextField southLatField = new JFormattedTextField(doubleFormatter);
        southLatField.setHorizontalAlignment(JTextField.RIGHT);
        binding.bind("southBound", southLatField);
        binding.bindEnabledState("southBound", false, "updateMode", true);
        panel.add(southLatField);
        panel.add(new JLabel(NonSI.DEGREE_ANGLE.toString()));
        panel.add(new JLabel("Pixel size Y:"));
        pixelSizeYField = new JFormattedTextField(doubleFormatter);
        pixelSizeYField.setHorizontalAlignment(JTextField.RIGHT);
        binding.bind("pixelSizeY", pixelSizeYField);
        binding.bindEnabledState("pixelSizeY", false, "updateMode", true);
        panel.add(pixelSizeYField);
        panel.add(pixelYUnit);

        return panel;
    }

    private JPanel createOrthorectifyPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Orthorectification"));

        final JCheckBox orthoCheckBox = new JCheckBox("Orthorectify input products");
        binding.bind("orthorectify", orthoCheckBox);
        binding.bindEnabledState("orthorectify", false, "updateMode", true);
        JComboBox demComboBox = new JComboBox(new DefaultComboBoxModel(demValueSet));
        binding.bind("elevationModelName", demComboBox);
        binding.bindEnabledState("elevationModelName", true, "orthorectify", true);
        binding.bindEnabledState("elevationModelName", false, "updateMode", true);
        layout.setCellColspan(0, 0, 2);
        panel.add(orthoCheckBox);

        layout.setCellWeightX(1, 0, 0.0);
        panel.add(new JLabel("Elevation model:"));
        layout.setCellWeightX(1, 1, 1.0);
        panel.add(demComboBox);
        return panel;
    }

    public void setReferenceProduct(Product product) {
        crsSelectionPanel.setReferenceProduct(product);
    }

    private void setMapBoundary(WorldMapPaneDataModel worldMapModel) {
        Product boundaryProduct;
        try {
            boundaryProduct = mosaicModel.getBoundaryProduct();
        } catch (Throwable ignored) {
            boundaryProduct = null;
        }
        worldMapModel.setSelectedProduct(boundaryProduct);
    }

    public void prepareShow() {
        crsSelectionPanel.prepareShow();
    }

    public void prepareHide() {
        crsSelectionPanel.prepareHide();
    }

    private class MapBoundsChangeListener implements PropertyChangeListener {

        private final List<String> knownProperties;
        private final WorldMapPaneDataModel worldMapModel;

        private MapBoundsChangeListener(WorldMapPaneDataModel worldMapModel) {
            this.worldMapModel = worldMapModel;
            knownProperties = Arrays.asList("westBound", "northBound", "eastBound", "southBound", "wkt");
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (knownProperties.contains(evt.getPropertyName())) {
                setMapBoundary(worldMapModel);
            }
        }

    }

    private static class DoubleFormatter extends JFormattedTextField.AbstractFormatter {

        private final DecimalFormat format;

        DoubleFormatter(String pattern) {
            final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
            format = new DecimalFormat(pattern, decimalFormatSymbols);

            format.setParseIntegerOnly(false);
            format.setParseBigDecimal(false);
            format.setDecimalSeparatorAlwaysShown(true);
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            return format.parse(text).doubleValue();
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            }
            return format.format(value);
        }
    }
}
