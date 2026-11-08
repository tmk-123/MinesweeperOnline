package minesweeperonline.client.ui;

import java.awt.*;

/** Bố cục FlowLayout tính chiều cao mong muốn theo cả các hàng xuống dòng khi cửa sổ hẹp. */
public final class WrapLayout extends FlowLayout {
    /** Cài hướng căn và khoảng cách giữa các component cho bố cục tự xuống dòng. */
    public WrapLayout(int alignment, int horizontalGap, int verticalGap) {
        super(alignment, horizontalGap, verticalGap);
    }

    /** Tính chiều cao theo số hàng thực tế sau khi xuống dòng, tránh cắt nội dung khi cửa sổ hẹp. */
    @Override public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int width = target.getWidth();
            int available = width > 0 ? width - insets.left - insets.right - 2 * getHgap() : Integer.MAX_VALUE;
            int rowWidth = 0;
            int rowHeight = 0;
            int contentWidth = 0;
            int contentHeight = 0;
            for (Component component : target.getComponents()) {
                if (!component.isVisible()) continue;
                Dimension size = component.getPreferredSize();
                int gap = rowWidth == 0 ? 0 : getHgap();
                if (rowWidth > 0 && rowWidth + gap + size.width > available) {
                    contentWidth = Math.max(contentWidth, rowWidth);
                    contentHeight += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                    gap = 0;
                }
                rowWidth += gap + size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            contentWidth = Math.max(contentWidth, rowWidth);
            contentHeight += rowHeight;
            return new Dimension(contentWidth + insets.left + insets.right + 2 * getHgap(),
                    contentHeight + insets.top + insets.bottom + 2 * getVgap());
        }
    }

    /** Dùng chiều cao đã tính theo dòng và cho phép container thu hẹp chiều ngang. */
    @Override public Dimension minimumLayoutSize(Container target) {
        Dimension size = preferredLayoutSize(target);
        size.width = 0;
        return size;
    }
}
