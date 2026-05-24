import Toybox.Lang;
import Toybox.WatchUi;

class SpotifyRemoteDelegate extends WatchUi.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    // Physical Top/Select button
    function onSelect() as Boolean {
        var view = WatchUi.getCurrentView()[0] as SpotifyRemoteView;
        if (view != null) { view.togglePlaybackVisual(); }
        
        sendBluetoothMessage("PLAY_PAUSE");
        return true;
    }
    
    // Physical Down button -> Switches Views
    function onNextPage() as Boolean {
        // Transition to the secondary Volume & Skip control touch panel
        WatchUi.pushView(
            new VolumeTouchView(), 
            new VolumeTouchDelegate(), 
            WatchUi.SLIDE_UP
        );
        return true;
    }

    // Helper function to send data to the phone companion app
    function sendRemoteCommand(commandString) as Void {
        var listener = new ConnectionListener();
        Communications.transmit(
            {"action" => commandString}, // The message payload
            null, 
            listener
        );
    }
    function sendBluetoothMessage(commandValue as String) as Void {
        Communications.transmit({"action" => commandValue}, null, new GarminConnectionListener());
    }

}

// Simple listener to track if the phone actually received the message
class ConnectionListener extends Communications.ConnectionListener {
    function onComplete() {
        System.println("Command sent successfully!");
    }
    function onError() {
        System.println("Transmission failed. Is the phone in range?");
    }
}