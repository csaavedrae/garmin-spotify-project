import Toybox.Lang;
import Toybox.WatchUi;
import Toybox.Communications;
using Toybox.System;

class MyConnectionListener extends Communications.ConnectionListener {

    function initialize() {
        ConnectionListener.initialize();
    }

    function onComplete() as Void {
        System.println("Message sent successfully");
    }

    function onError() as Void {
        System.println("Message failed");
    }
}

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
        var listener = new MyConnectionListener();

        Communications.transmit(
            {"action" => commandString},
            null,
            listener
            );
    }
    function sendBluetoothMessage(commandValue) as Void {
        var listener = new MyConnectionListener();
        
        Communications.transmit(
            {"action" => commandValue},
            null,
            listener
            );
    }
}

// Simple listener to track if the phone actually received the message
class ConnectionListener {
    function onComplete() {
        System.println("Command sent successfully!");
    }
    function onError() {
        System.println("Transmission failed. Is the phone in range?");
    }
}