package com.example.telega;

import org.springframework.stereotype.Service;
import org.telegram.api.TLConfig;
import org.telegram.api.auth.TLSentCode;
import org.telegram.api.document.TLDocument;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.api.functions.auth.TLRequestAuthSendCode;
import org.telegram.api.functions.auth.TLRequestAuthSignIn;
import org.telegram.api.functions.channels.TLRequestChannelsExportMessageLink;
import org.telegram.api.functions.channels.TLRequestChannelsGetFullChannel;
import org.telegram.api.functions.channels.TLRequestChannelsGetMessages;
import org.telegram.api.functions.help.TLRequestHelpGetConfig;
import org.telegram.api.functions.messages.TLRequestMessagesGetDialogs;
import org.telegram.api.functions.messages.TLRequestMessagesSendMedia;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.document.TLInputDocument;
import org.telegram.api.input.media.TLInputMediaDocument;
import org.telegram.api.input.media.TLInputMediaPhoto;
import org.telegram.api.input.peer.TLInputPeerChannel;
import org.telegram.api.input.peer.TLInputPeerUser;
import org.telegram.api.input.photo.TLInputPhoto;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLExportedMessageLink;
import org.telegram.api.message.TLMessage;
import org.telegram.api.message.media.TLAbsMessageMedia;
import org.telegram.api.message.media.TLMessageMediaDocument;
import org.telegram.api.message.media.TLMessageMediaPhoto;
import org.telegram.api.messages.TLAbsMessages;
import org.telegram.api.messages.TLMessagesChatFull;
import org.telegram.api.messages.dialogs.TLAbsDialogs;
import org.telegram.api.photo.TLPhoto;
import org.telegram.api.updates.TLAbsUpdates;
import org.telegram.bot.kernel.engine.MemoryApiState;
import org.telegram.tl.TLIntVector;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BotServiceImpl implements IBotService {
    private final AbsApiState apiState;
    private final TelegramApi api;

    public BotServiceImpl() {
        this.apiState = new MemoryApiState("telegram.sta");
        this.api = prepareTelegramApi(apiState);
        auth();
    }

    private static TelegramApi prepareTelegramApi(AbsApiState apiState) {
        final AppInfo appInfo = new AppInfo(1439024, "WEB", "Linux", "0.0.1", "EN");
        return new TelegramApi(apiState, appInfo, new ApiCallback() {
            @Override
            public void onAuthCancelled(TelegramApi api1) {
                System.err.println("onAuthCancelled");
            }

            @Override
            public void onUpdatesInvalidated(TelegramApi api1) {
                System.err.println("onUpdatesInvalidated");
            }

            @Override
            public void onUpdate(TLAbsUpdates updates) {
                System.err.println("onUpdate");
            }
        });
    }

    private void auth() {
        try {
            if (!apiState.isAuthenticated()) {
                apiState.resetAuth();
                final TLConfig config = api.doRpcCallNonAuth(new TLRequestHelpGetConfig());
                apiState.setPrimaryDc(config.getThisDc());
                apiState.updateSettings(config);

                final TLRequestAuthSendCode tlRequestAuthSendCode = new TLRequestAuthSendCode();
                tlRequestAuthSendCode.setApiId(1439024);
                tlRequestAuthSendCode.setApiHash("1897a16af380e91182b2661376b7b40");
                final String phoneNumber = "37525443163";
                tlRequestAuthSendCode.setPhoneNumber(phoneNumber);
                TLSentCode tlSentCode;
                try {
                    tlSentCode = api.doRpcCallNonAuth(tlRequestAuthSendCode);
                } catch (Exception e) {
                    api.switchToDc(2);
                    tlSentCode = api.doRpcCallNonAuth(tlRequestAuthSendCode);
                }
                //


                final TLRequestAuthSignIn tlRequestAuthSignIn = new TLRequestAuthSignIn();
                tlRequestAuthSignIn.setPhoneNumber(phoneNumber);
                tlRequestAuthSignIn.setPhoneCodeHash(tlSentCode.getPhoneCodeHash());

                api.doRpcCallNonAuth(tlRequestAuthSignIn);
                apiState.setAuthenticated(apiState.getPrimaryDc(), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TLAbsDialogs getDialogs() {
        final TLRequestMessagesGetDialogs method = new TLRequestMessagesGetDialogs();
        final TLInputPeerChannel offsetPeer = new TLInputPeerChannel();
        method.setOffsetPeer(offsetPeer);
        try {
            return api.doRpcCall(method);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public TLMessagesChatFull getFullInfoChanel(Integer chanelId, Long hash) {
        try {
            final TLRequestChannelsGetFullChannel method = new TLRequestChannelsGetFullChannel();
            final TLInputChannel channel = new TLInputChannel();
            channel.setChannelId(chanelId);
            channel.setAccessHash(hash);
            method.setChannel(channel);
            return api.doRpcCall(method);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public TLInputPeerUser getDataBotPeer() {
        final TLInputPeerUser value = new TLInputPeerUser();
        value.setUserId(1376909918);
        value.setAccessHash(1766018361576822973L);
        return value;
    }

    @Override
    public TLExportedMessageLink getLink(Integer chanelId, Long hash) {
        try {
            final TLRequestChannelsExportMessageLink method = new TLRequestChannelsExportMessageLink();
            final TLInputChannel channel = new TLInputChannel();
            channel.setChannelId(chanelId);
            channel.setAccessHash(hash);
            method.setChannel(channel);
            return api.doRpcCall(method);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<Integer, List<TLMessage>> getMessages(TLInputChannel channel, Integer max) {
        try {
            final TLRequestChannelsGetMessages getMessagesRequest = new TLRequestChannelsGetMessages();
            getMessagesRequest.setChannel(channel);
            final TLIntVector id = new TLIntVector();
            for (int i = max; i >= 0; i--) {
                id.add(i);
            }
            getMessagesRequest.setId(id);
            final TLAbsMessages tlAbsMessages = api.doRpcCall(getMessagesRequest);
            return tlAbsMessages.getMessages()
                    .stream()
                    .filter(f -> f instanceof TLMessage)
                    .map(f -> (TLMessage) f)
                    .collect(Collectors.groupingBy(TLMessage::getDate))
                    .entrySet()
                    .stream()
                    .filter(f -> f.getValue().size() == 3)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void sendCrappedMessagesToBot(TLInputPeerUser botPeer, List<TLMessage> messages) {


        final String groupId = UUID.randomUUID().toString().replaceAll("-", "");
        try {
            for (TLAbsMessage message : messages) {
                final TLRequestMessagesSendMedia mediaRequest = new TLRequestMessagesSendMedia();
                final TLAbsMessageMedia media = ((TLMessage) message).getMedia();

                mediaRequest.setPeer(botPeer);
                mediaRequest.setRandomId(new Random().nextLong());
                mediaRequest.setReplyToMsgId(2);

                if (media instanceof TLMessageMediaDocument) {
                    final TLInputMediaDocument value = new TLInputMediaDocument();
                    final TLMessageMediaDocument temp = (TLMessageMediaDocument) media;
                    final TLDocument document = (TLDocument) temp.getDocument();
                    value.setCaption(temp.getCaption() + " #groupId" + groupId);

                    final TLInputDocument value1 = new TLInputDocument();
                    value1.setId(document.getId());
                    value1.setAccessHash(document.getAccessHash());
                    value.setId(value1);
                    mediaRequest.setMedia(value);

                } else if (media instanceof TLMessageMediaPhoto) {
                    final TLInputMediaPhoto value = new TLInputMediaPhoto();
                    final TLMessageMediaPhoto temp = (TLMessageMediaPhoto) media;
                    final TLPhoto document = (TLPhoto) temp.getPhoto();
                    value.setCaption(temp.getCaption() + " #groupId" + groupId);

                    final TLInputPhoto value1 = new TLInputPhoto();
                    value1.setId(document.getId());
                    value1.setAccessHash(document.getAccessHash());
                    value.setId(value1);
                    mediaRequest.setMedia(value);
                }
                api.doRpcCall(mediaRequest);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
