package com.example.telega;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.input.peer.TLInputPeerUser;
import org.telegram.api.message.TLExportedMessageLink;
import org.telegram.api.messages.dialogs.TLAbsDialogs;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MyDataBot extends TelegramLongPollingBot {

    private Map<String, List<InputMedia>> storage = new HashMap<>();

    @Autowired
    private IBotService botService;

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);

//        final TLInputPeerUser botPeer = new TLInputPeerUser();
//        botPeer.setUserId(1107620097);
//        botPeer.setAccessHash(-4051983353036532983L);

        if (!CollectionUtils.isEmpty(storage)) {
            storage.values()
                    .stream()
                    .filter(f -> f.size() == 3)
                    .map(inputMedia -> {
                        final SendMediaGroup sendMediaGroup = new SendMediaGroup();
                        sendMediaGroup.setChatId(-1001349101482L);
                        sendMediaGroup.setMedia(inputMedia.stream().sorted(Comparator.comparing(inputMedia1 -> !inputMedia1.getType().equalsIgnoreCase("Photo"))).collect(Collectors.toList()));
                        return sendMediaGroup;
                    })
                    .forEach(f -> {
                        try {
                            execute(f);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
             if (update.getMessage().hasPhoto()) {
                final Long chatId = update.getMessage().getChatId();
                final String caption = update.getMessage().getCaption();
                final Optional<MessageEntity> groupId = update.getMessage().getCaptionEntities()
                        .stream()
                        .filter(f -> f.getText().toLowerCase().contains("groupid"))
                        .findFirst();
                if (groupId.isPresent()) {
                    final String text = groupId.get().getText();
                    final List<PhotoSize> photo = update.getMessage().getPhoto();
                    photo
                            .stream()
                            .max(Comparator.comparing(PhotoSize::getFileSize))
                            .ifPresent(
                                    f -> {
                                        final String key = text + "," + chatId;
                                        try {
                                            final GetFile method = new GetFile();
                                            method.setFileId(f.getFileId());
                                            final File execute = execute(method);
                                            final String fileUrl = execute.getFileUrl(getBotToken());
                                            URL url = new URL(fileUrl);
                                            InputStream in = new BufferedInputStream(url.openStream());
                                            storage.putIfAbsent(key, Lists.newArrayList());
                                            if (Objects.nonNull(storage.get(key))) {
                                                final InputMediaPhoto mediaPhoto = new InputMediaPhoto();
                                                mediaPhoto.setMedia(in, "image.jpg");
                                                mediaPhoto.setCaption(caption.replaceAll(text, ""));
                                                storage.get(key).add(mediaPhoto);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                            );
                }


            } else if (update.getMessage().hasVideo()) {

                final Long chatId = update.getMessage().getChatId();
                final String caption = update.getMessage().getCaption();
                final Optional<MessageEntity> groupId = update.getMessage().getCaptionEntities()
                        .stream()
                        .filter(f -> f.getText().toLowerCase().contains("groupid"))
                        .findFirst();
                if (groupId.isPresent()) {
                    final String text = groupId.get().getText();

                    final Video video = update.getMessage().getVideo();
                    if (Objects.nonNull(video)) {
                        final String key = text + "," + chatId;
                        storage.putIfAbsent(key, Lists.newArrayList());
                        if (Objects.nonNull(storage.get(key))) {
                            final InputMediaVideo mediaVideo = new InputMediaVideo(video.getFileId(), caption.replaceAll(text, ""));
                            storage.get(key).add(mediaVideo);
                        }
                    }

                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        // TODO
        return "Kudr9tovdatabot";
    }

    @Override
    public String getBotToken() {
        // TODO
        return "1376909918:AAHoyKFwHdcmuxVNllv4-SkTpX4esEPzzCo";
    }

}
