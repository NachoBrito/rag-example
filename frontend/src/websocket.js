
var webSocket = new WebSocket("ws://" +
    location.hostname +
    ":" +
    location.port +
    "/ws/events");

webSocket.onclose = function () { alert("WebSocket connection closed") };
webSocket.onmessage = function (msg) { 
    let data = JSON.parse(msg.data);
    console.info(data);
    
    if(data.message_type){
        let event = new CustomEvent(data.message_type.toLowerCase() + "-received", {
            detail: {
                message: data,
                timestamp: Date.now()
            },
        });    
        console.info("Dispatching event:");
        console.info(event);
        document.dispatchEvent(event);
    }
    else{
        console.error("Cannot process message! ")
    }
};

//Send a message if it's not empty, then clear the input field
function sendMessage(message) {
    if (message !== "") {
        webSocket.send(message);
    }
}

function handleUserMessage(event){
    console.log(event);
    sendMessage(JSON.stringify(event.detail));
}

document.addEventListener("user-talked", handleUserMessage);

