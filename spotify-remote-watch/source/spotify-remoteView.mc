import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Lang;

class SpotifyRemoteView extends WatchUi.View {
    private var isPlaying as Boolean = false;
    private var currentTrack as String = "Waiting for Phone...";
    private var currentArtist as String = "";

    function initialize() {
        View.initialize();
    }

    function onUpdate(dc as Dc) as Void {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();

        var centerX = dc.getWidth() / 2;
        var centerY = dc.getHeight() / 2;

        // Draw Meta Text (Spotify Dynamic Updates)
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, centerY - 50, Graphics.FONT_MEDIUM, currentTrack, Graphics.TEXT_JUSTIFY_CENTER);
        dc.drawText(centerX, centerY - 25, Graphics.FONT_SMALL, currentArtist, Graphics.TEXT_JUSTIFY_CENTER);

        // Center UI State Widget (Play / Pause Status Indicator)
        dc.setColor(0x1DB954, Graphics.COLOR_TRANSPARENT);
        if (isPlaying) {
            dc.fillRectangle(centerX - 8, centerY + 20, 5, 18);
            dc.fillRectangle(centerX + 3, centerY + 20, 5, 18);
        } else {
            var pts = [[centerX - 5, centerY + 18], [centerX - 5, centerY + 38], [centerX + 12, centerY + 28]];
            dc.fillPolygon(pts);
        }
    }

    // Public method called by delegate/system to change track information over the air
    function updateTrackInfo(trackName as String, artistName as String) as Void {
        currentTrack = trackName;
        currentArtist = artistName;
        WatchUi.requestUpdate();
    }

    function togglePlaybackVisual() as Void {
        isPlaying = !isPlaying;
        WatchUi.requestUpdate();
    }
}
