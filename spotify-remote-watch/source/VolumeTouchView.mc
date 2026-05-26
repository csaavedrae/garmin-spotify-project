import Toybox.WatchUi;
import Toybox.Graphics;
using Toybox.Graphics;

class VolumeTouchView extends WatchUi.View {

    function initialize() {
        View.initialize();
    }

    function onUpdate(dc as Dc) as Void {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();

        var width = dc.getWidth();
        var height = dc.getHeight();
        var centerX = width / 2;
        var centerY = height / 2;

        // Draw Horizontal Separation Line Splitter
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_TRANSPARENT);
        dc.setPenWidth(4);
        dc.drawLine(0, centerY, width, centerY);

        // Render Interaction Icons 
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        
        // Upper Zone: Volume Up Input UI indicator
        dc.drawText(centerX, centerY - 60, Graphics.FONT_MEDIUM, "+ VOLUME", Graphics.TEXT_JUSTIFY_CENTER);
        
        // Lower Zone: Volume Down Input UI indicator
        dc.drawText(centerX, centerY + 25, Graphics.FONT_MEDIUM, "- VOLUME", Graphics.TEXT_JUSTIFY_CENTER);

        // Sides: Track Skipping cues mapping physical controls
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(width - 35, centerY - 10, Graphics.FONT_GLANCE, "Skip >", Graphics.TEXT_JUSTIFY_RIGHT);
        dc.drawText(35, centerY - 10, Graphics.FONT_GLANCE, "< Prev", Graphics.TEXT_JUSTIFY_LEFT);
    }
}