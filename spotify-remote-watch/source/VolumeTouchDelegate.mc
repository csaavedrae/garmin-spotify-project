import Toybox.WatchUi;
import Toybox.Communications;

class VolumeTouchDelegate extends WatchUi.InputDelegate {

    function initialize() {
        InputDelegate.initialize();
    }

    // INTERCEPT SCREEN TOUCHES
    function onTap(clickEvent as WatchUi.ClickEvent) {
        var coordinates = clickEvent.getCoordinates();
        var tapY = coordinates[1]; // Get Y axis coordinate position

        // Target active view dimension properties
        //var viewHeight = WatchUi.getCurrentView()[0].getHeight(); Commenting for debugging
        var viewHeight = 454;

        if (tapY < (viewHeight / 2)) {
            // Tapped Top Half of Watch Screen
            sendBluetoothMessage("VOLUME_UP");
        } else {
            // Tapped Bottom Half of Watch Screen
            sendBluetoothMessage("VOLUME_DOWN");
        }
        return true;
    }

    // INTERCEPT PHYSICAL DEVICE BUTTON PRESSES
    function onKey(keyEvent as WatchUi.KeyEvent){
        var key = keyEvent.getKey();

        if (key == WatchUi.KEY_ENTER) { 
            // Hardware Select Key skips Track Forward
            sendBluetoothMessage("NEXT_TRACK");
            return true;
        } 
        else if (key == WatchUi.KEY_DOWN) {
            // Hardware Down Key steps Track Backward
            sendBluetoothMessage("PREV_TRACK");
            return true;
        }
        else if (key == WatchUi.KEY_ESC) {
            // Hardware Back Key safely exits our volume layout stack cleanly
            WatchUi.popView(WatchUi.SLIDE_DOWN);
            return true;
        }
        return false;
    }

    function sendBluetoothMessage(commandValue) as Void {
        Communications.transmit(
            {"action" => commandValue}, 
            null,
            null as Communications.ConnectionListener
         );
    }
}