/**
 * ChromeCast receiver application for CastDice.
 */
 window.onload = function() {

   window.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();

   // enable debug logging in Cast receiver library
   //cast.receiver.logger.setLevelValue(cast.receiver.LoggerLevel.DEBUG);

   // handle 'ready' event
   castReceiverManager.onReady = function(event) {
      //console.log('Received Ready event: ' + JSON.stringify(event.data));
      window.castReceiverManager.setApplicationState("Application status is ready...");
   };

   // handle 'sender disconnected' event
   castReceiverManager.onSenderDisconnected = function(event) {
      if (window.castReceiverManager.getSenders().length == 0) {
         window.close();
      }
   };

   // create CastMessageBus to handle messages for a custom namespace
   window.messageBus =
      window.castReceiverManager.getCastMessageBus('urn:x-cast:com.honu.dicecast');

   // handler for the CastMessageBus message event
   window.messageBus.onMessage = function(event) {

      // parse the message
      var message = JSON.parse(event.data);

      // update the display
      updateDisplay(message.text, message.die1, message.die2);

      // inform all senders on the CastMessageBus of the incoming message event
      // sender message listener will be invoked
      window.messageBus.send(event.senderId, event.data);
   }

   // initialize the CastReceiverManager with an application status message
   window.castReceiverManager.start({statusText: "Application is starting"});
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

   document.getElementById("message").innerHTML=text;
   document.getElementById("die1").src=imgs[die1];
   document.getElementById("die2").src=imgs[die2];

   window.castReceiverManager.setApplicationState(text);
};

