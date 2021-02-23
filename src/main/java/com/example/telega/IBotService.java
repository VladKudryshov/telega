package com.example.telega;

import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.peer.TLInputPeerUser;
import org.telegram.api.message.TLExportedMessageLink;
import org.telegram.api.message.TLMessage;
import org.telegram.api.messages.TLMessagesChatFull;
import org.telegram.api.messages.dialogs.TLAbsDialogs;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IBotService {
    void sendCrappedMessagesToBot(TLInputPeerUser botPeer, List<TLMessage> messages);

    Map<Integer, List<TLMessage>> getMessages(TLInputChannel channel, Integer max);

    TLAbsDialogs getDialogs();

    TLMessagesChatFull getFullInfoChanel(Integer chanelId, Long hash);

    TLExportedMessageLink getLink(Integer chanelId, Long hash);

    TLInputPeerUser getDataBotPeer();
}
