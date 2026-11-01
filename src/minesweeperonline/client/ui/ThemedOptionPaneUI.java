package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import java.awt.*;

/** Apply the same readable buttons to every Swing message and confirmation dialog. */
public final class ThemedOptionPaneUI extends BasicOptionPaneUI {
    public static ComponentUI createUI(JComponent component) {
        return new ThemedOptionPaneUI();
    }

    @Override
    protected Container createButtonArea() {
        Container area = super.createButtonArea();
        styleButtons(area);
        return area;
    }

    private void styleButtons(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) {
                UITheme.styleOutlineButton(button, UITheme.BORDER_CARD,
                        UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
            } else if (component instanceof Container child) {
                styleButtons(child);
            }
        }
    }
}
