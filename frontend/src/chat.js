import {v4 as uuidv4} from 'uuid';

const talkButton = document.getElementById("remo-textbox-ptt");
const textBox = document.getElementById("remo-textbox-field");

textBox.focus();
talkButton.addEventListener("click", pttButtonClicked);


function pttButtonClicked(userEvent){
    userEvent.preventDefault();
    const text = textBox.value;
    if(text === ""){
        console.debug("Nothing to send.");
        return;
    }
    const event = new CustomEvent("user-talked", {
        detail: {
            queryId: uuidv4(),
            message: text,
            timestamp: Date.now()
        },
    });
    document.dispatchEvent(event);
    textBox.value = "";
    textBox.focus();
}

function handleUserMessageReceived(event) {
    const conversationContainer = document.getElementById('remo-conversation');
    const queryId = event.detail.queryId;
    const text = event.detail.message;

    let messageContainer = document.getElementById('user-message-'+queryId);
    if(!messageContainer){
        messageContainer = document.createElement("div");
        messageContainer.classList.add("chat","user");
        messageContainer.id = 'user-message-'+queryId;
        conversationContainer.appendChild(messageContainer);
    }
    messageContainer.innerHTML = text;
}

function handleChatResponseReceived(event){
    console.info(event);
    const conversationContainer = document.getElementById('remo-conversation');
    const message = event.detail.message;
    const queryId = message.queryId;
    const isComplete = message.isComplete;
    const tokens = message.tokens;

    let messageContainer = document.getElementById('response-'+queryId);
    if(!messageContainer){
        messageContainer = document.createElement("div");
        messageContainer.classList.add("chat", "bot");
        messageContainer.id = 'response-'+queryId;
        conversationContainer.appendChild(messageContainer);
    }
    if(tokens){
        messageContainer.innerHTML += tokens;
    }
    if(isComplete){
        messageContainer.classList.add("complete");
    }
    

//    let proposals = event.detail.message.proposals;
//    proposals.forEach(addProposal);
}

document.addEventListener("response-tokens-received", handleChatResponseReceived);
document.addEventListener("user-talked", handleUserMessageReceived);
