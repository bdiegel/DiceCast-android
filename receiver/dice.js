/**
 * ChromeCast receiver application for CastDice.
 */
 window.onload = function() {

   window.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();

   // enable debug logging in Cast receiver library
   //cast.receiver.logger.setLevelValue(cast.receiver.LoggerLevel.DEBUG);

   console.log('Starting Receiver Manager');

   // handle 'ready' event
   castReceiverManager.onReady = function(event) {
      console.log('Received Ready event: ' + JSON.stringify(event.data));
      window.castReceiverManager.setApplicationState("Application status is ready...");
   };

   // handle 'sender connected' event
   castReceiverManager.onSenderConnected = function(event) {
      console.log('Received Sender Connected event: ' + event.data);
      console.log(window.castReceiverManager.getSender(event.data).userAgent);
   };

   // handle 'sender disconnected' event
   castReceiverManager.onSenderDisconnected = function(event) {
      console.log('Received Sender Disconnected event: ' + event.data);
      if (window.castReceiverManager.getSenders().length == 0) {
         window.close();
      }
   };

   // create CastMessageBus to handle messages for a custom namespace
   window.messageBus =
      window.castReceiverManager.getCastMessageBus('urn:x-cast:com.dice.app.DiceCast');

   // handler for the CastMessageBus message event
   window.messageBus.onMessage = function(event) {

      console.log('Message received [' + event.senderId + ']: ' + JSON.stringify(event.data));

      // parse the message
      var message = JSON.parse(event.data);
      console.log('Message parsed: [text: ' + message.text + ' die1: ' + message.die1 + ' die2: ' + message.die2 + ']');

      // update the display
      updateDisplay(message.text, message.die1, message.die2);

      // inform all senders on the CastMessageBus of the incoming message event
      // sender message listener will be invoked
      window.messageBus.send(event.senderId, event.data);
   }

   // initialize the CastReceiverManager with an application status message
   window.castReceiverManager.start({statusText: "Application is starting"});
   console.log('Receiver Manager started');
};


// utility function to display the text message in the input field
function updateDisplay(text, die1, die2) {
   var imgs = {
      1: 'die-6sided-1.svg',
      2: 'die-6sided-2.svg',
      3: 'die-6sided-3.svg',
      4: 'die-6sided-4.svg',
      5: 'die-6sided-5.svg',
      6: 'die-6sided-6.svg'
   };

   console.log('Update display: ' + text);
   document.getElementById("message").innerHTML=text;
   document.getElementById("die1").src=imgs[die1];
   document.getElementById("die2").src=imgs[die2];

   window.castReceiverManager.setApplicationState(text);
};

