package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import java.awt.Component;


interface ColorManipulationChildForm {
    void handleFormShown(ProductSceneView productSceneView);

    void handleFormHidden(ProductSceneView productSceneView);

    void updateFormModel(ProductSceneView productSceneView);

    Component getContentPanel();

    AbstractButton[] getButtons();

    RasterDataNode[] getRasters();

    MoreOptionsForm getMoreOptionsForm();
}
