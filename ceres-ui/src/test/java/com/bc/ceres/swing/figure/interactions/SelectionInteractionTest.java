package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureEditor2;
import junit.framework.TestCase;

import javax.swing.JPanel;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class SelectionInteractionTest extends TestCase {
    public void testModeChange() {
        DefaultShapeFigure f1 = new DefaultShapeFigure(new Rectangle(10, 10, 10, 10), true, new DefaultFigureStyle());
        DefaultShapeFigure f2 = new DefaultShapeFigure(new Rectangle(30, 10, 10, 10), true, new DefaultFigureStyle());
        FigureEditor fe = new DefaultFigureEditor2(new JPanel());
        fe.getFigureCollection().addFigure(f1);
        fe.getFigureCollection().addFigure(f2);

        Interactor sa = new SelectionInteractor();
        sa.activate();

        // Test initial state: nothing selected
        assertEquals(0, fe.getFigureSelection().getFigureCount());
        assertEquals(0, fe.getFigureSelection().getSelectionStage());

        // Click at 0,0 --> still no selection
        click(sa, fe, 0, 0, 0);
        assertEquals(0, fe.getFigureSelection().getFigureCount());
        assertEquals(0, fe.getFigureSelection().getSelectionStage());

        // Click figure 1 --> select it
        click(sa, fe, 10 + 5, 10 + 5, 0);
        assertEquals(1, fe.getFigureSelection().getFigureCount());
        assertEquals(1, fe.getFigureSelection().getSelectionStage());
        assertSame(f1, fe.getFigureSelection().getFigure(0));

        // Click again figure 1 --> selection level increased
        click(sa, fe, 11 + 5, 10 + 5, 0);
        assertEquals(1, fe.getFigureSelection().getFigureCount());
        assertEquals(2, fe.getFigureSelection().getSelectionStage());
        assertSame(f1, fe.getFigureSelection().getFigure(0));

        click(sa, fe, 10 + 5, 10 + 5, 0);
        assertEquals(1, fe.getFigureSelection().getFigureCount());
        assertEquals(3, fe.getFigureSelection().getSelectionStage());
        assertSame(f1, fe.getFigureSelection().getFigure(0));

        click(sa, fe, 0, 0, 0);
        assertEquals(0, fe.getFigureSelection().getFigureCount());
        assertEquals(0, fe.getFigureSelection().getSelectionStage());

        click(sa, fe, 30 + 5, 10 + 5, 0);
        assertEquals(1, fe.getFigureSelection().getFigureCount());
        assertEquals(1, fe.getFigureSelection().getSelectionStage());
        assertSame(f2, fe.getFigureSelection().getFigure(0));

        click(sa, fe, 10 + 5, 10 + 5, MouseEvent.CTRL_MASK);
        assertEquals(2, fe.getFigureSelection().getFigureCount());
        assertEquals(2, fe.getFigureSelection().getSelectionStage());
        assertSame(f2, fe.getFigureSelection().getFigure(0));
        assertSame(f1, fe.getFigureSelection().getFigure(1));

        click(sa, fe, 0, 0, 0);
        assertEquals(0, fe.getFigureSelection().getFigureCount());
        assertEquals(0, fe.getFigureSelection().getSelectionStage());

        // Select all figures in rect(0,0,100,100)
        startDrag(sa, fe, 0, 0, 0);
        endDrag(sa, fe, 100, 100, 0);
        assertEquals(2, fe.getFigureSelection().getFigureCount());
        assertEquals(2, fe.getFigureSelection().getSelectionStage());
        assertSame(f1, fe.getFigureSelection().getFigure(0));
        assertSame(f2, fe.getFigureSelection().getFigure(1));

        // Move selection to point(200,200)
        startDrag(sa, fe, 10 + 5, 10 + 5, 0);
        endDrag(sa, fe, 200, 200, 0);
        assertEquals(2, fe.getFigureSelection().getFigureCount());
        assertEquals(2, fe.getFigureSelection().getSelectionStage());
        assertSame(f1, fe.getFigureSelection().getFigure(0));
        assertSame(f2, fe.getFigureSelection().getFigure(1));
        assertEquals(new Rectangle2D.Double(195.0, 195.0, 10.0, 10.0), f1.getBounds());
        assertEquals(new Rectangle2D.Double(215.0, 195.0, 10.0, 10.0), f2.getBounds());

        // todo - test drag selected figure (--> move)
        // todo - test drag selected handle (--> scale/rotate/move vertex)

        sa.deactivate();
    }

    private void click(Interactor sa, FigureEditor fe, int x, int y, int modifiers) {
        MouseEvent event;
        event = createEvent(fe, modifiers, x, y);
        sa.mousePressed(event);
        sa.mouseReleased(event);
    }

    private void startDrag(Interactor sa, FigureEditor fe, int x, int y, int modifiers) {
        MouseEvent event;
        event = createEvent(fe, modifiers, x, y);
        sa.mousePressed(event);
        sa.mouseDragged(event);
    }

    private void endDrag(Interactor sa, FigureEditor fe, int x, int y, int modifiers) {
        MouseEvent event;
        event = createEvent(fe, modifiers, x, y);
        sa.mouseDragged(event);
        sa.mouseReleased(event);
    }

    private MouseEvent createEvent(FigureEditor fe, int modifiers, int x, int y) {
        return new MouseEvent(fe.getEditorComponent(), 0, 0, modifiers, x, y, 1, false);
    }
}
