/*
 * Created at 17.07.2004 12:13:58
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.framework.ui.tool.impl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

import javax.swing.ImageIcon;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.draw.Drawable;
import org.esa.beam.framework.ui.product.LayerDisplay;
import org.esa.beam.framework.ui.tool.AbstractTool;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

import com.bc.ceres.grender.Viewport;
import com.bc.swing.GraphicsPane;

public class ZoomTool extends AbstractTool {
    private int _viewportX;
    private int _viewportY;
    private Graphics _graphics;
    private final Rectangle _zoomRect = new Rectangle();

    /**
     * Gets a thing that can be drawn while the tool is working.
     *
     * @return always <code>null</code>
     */
    public Drawable getDrawable() {
        return null;
    }

    public void mousePressed(ToolInputEvent e) {
        _graphics = e.getComponent().getGraphics();
        _viewportX = e.getMouseEvent().getX();
        _viewportY = e.getMouseEvent().getY();
        setZoomRect(e);
    }

    public void mouseDragged(ToolInputEvent e) {
        if (_graphics == null) {
            return;
        }
        _graphics.setXORMode(Color.white);
        if (!_zoomRect.isEmpty()) {
            drawZoomRect();
        }
        setZoomRect(e);
        drawZoomRect();
        _graphics.setPaintMode();
    }

    private void setZoomRect(ToolInputEvent e) {
        int x = _viewportX;
        int y = _viewportY;
        int w = e.getMouseEvent().getX() - x;
        int h = e.getMouseEvent().getY() - y;
        if (w < 0) {
            w = -w;
            x -= w;
        }
        if (h < 0) {
            h = -h;
            y -= h;
        }
        _zoomRect.setBounds(x, y, w, h);
    }

    public void mouseReleased(ToolInputEvent e) {
        if (_graphics == null) {
            return;
        }
        Component component = e.getComponent();
        if (component instanceof GraphicsPane) {
            GraphicsPane graphicsPane = (GraphicsPane) component;
            
            if (!_zoomRect.isEmpty()) {
                graphicsPane.zoom(new Rectangle2D.Double(graphicsPane.viewToModelX(_zoomRect.x),
                        graphicsPane.viewToModelY(_zoomRect.y),
                        graphicsPane.viewToModelLength(_zoomRect.width),
                        graphicsPane.viewToModelLength(_zoomRect.height)));
            } else {
                boolean zoomOut = e.getMouseEvent().isControlDown() || e.getMouseEvent().getButton() != 1;
                final double viewScaleOld = graphicsPane.getViewModel().getViewScale();
                final double viewScaleNew = zoomOut ? viewScaleOld / 1.6 : viewScaleOld * 1.6;
                graphicsPane.zoom(graphicsPane.viewToModelX(_zoomRect.x),
                        graphicsPane.viewToModelY(_zoomRect.y),
                        viewScaleNew);
            }
        } else if (component instanceof LayerDisplay) {
            LayerDisplay layerDisplay = (LayerDisplay) component;
            Viewport viewport = layerDisplay.getViewport();
            if (!_zoomRect.isEmpty()) {
                AffineTransform viewToModelTransform = viewport.getViewToModelTransform();
                Point2D viewP = new Point2D.Double(_zoomRect.x, _zoomRect.y);
                System.out.println("v1="+viewP);
                Point2D modelP = viewToModelTransform.transform(viewP, null);
                System.out.println("m1="+modelP);
                final double modelX = modelP.getX();
                final double modelY = modelP.getY();
                viewP.setLocation(_zoomRect.x+_zoomRect.width, _zoomRect.y+_zoomRect.height);
                System.out.println("v2="+viewP);
                viewToModelTransform.transform(viewP, modelP);
                System.out.println("m2="+modelP);
                final double modelW = modelP.getX() - modelX;
                final double modelH = modelP.getY() - modelY;
                Rectangle2D modelRect = new Rectangle2D.Double(modelX, modelY, modelW, modelH);
                
                Shape transformedShape = viewToModelTransform.createTransformedShape(_zoomRect);
                Rectangle2D bounds2D = transformedShape.getBounds2D();
                System.out.println("zoom: view="+_zoomRect);
                System.out.println("zoom: model="+modelRect);
                
                System.out.println("zoom: model2="+bounds2D);
                viewport.zoom(bounds2D);
            } else {
                boolean zoomOut = e.getMouseEvent().isControlDown() || e.getMouseEvent().getButton() != 1;
                final double viewScaleOld = viewport.getZoomFactor();
                final double viewScaleNew = zoomOut ? viewScaleOld / 1.6 : viewScaleOld * 1.6;
                viewport.zoom(viewScaleNew);
            }
        }
        _graphics.dispose();
        _graphics = null;
        _zoomRect.setBounds(0, 0, 0, 0);
    }


    public Cursor getCursor() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "pinCursor";
        ImageIcon icon = UIUtils.loadImageIcon("cursors/ZoomTool.gif");

        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(icon.getIconWidth(), icon.getIconHeight());
        Point hotSpot = new Point((8 * bestCursorSize.width) / icon.getIconWidth(),
                                  (8 * bestCursorSize.height) / icon.getIconHeight());

        return defaultToolkit.createCustomCursor(icon.getImage(), hotSpot, cursorName);
    }

    private void drawZoomRect() {
        _graphics.drawRect(_zoomRect.x, _zoomRect.y, _zoomRect.width, _zoomRect.height);
    }
}
