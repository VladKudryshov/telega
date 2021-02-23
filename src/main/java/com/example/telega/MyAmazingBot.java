package com.example.telega;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.message.TLMessage;
import org.telegram.api.messages.dialogs.TLAbsDialogs;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class MyAmazingBot extends TelegramLongPollingBot {

    private Map<String, List<InputMedia>> storage = new HashMap<>();

    private Map<String, TLChannel> chanels = new HashMap<>();

    private AtomicReference<TLChannel> choosedChanel = new AtomicReference<>();

    @Autowired
    private IBotService botService;

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);

        final Map<String, List<Message>> collect = updates.stream()
                .filter(f -> Objects.nonNull(f.getChannelPost()))
                .map(Update::getChannelPost)
                .collect(Collectors.groupingBy(Message::getMediaGroupId));

        if (!CollectionUtils.isEmpty(collect)) {
            collect.forEach((key, value) ->
                    value.forEach(r -> {
                        if (r.hasPhoto()) {
                            r.getPhoto()
                                    .stream()
                                    .max(Comparator.comparing(PhotoSize::getFileSize))
                                    .ifPresent(
                                            f -> {
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
                                                        mediaPhoto.setCaption(r.getCaption());
                                                        storage.get(key).add(mediaPhoto);
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                    );
                        } else if (r.hasVideo()) {
                            final Video video = r.getVideo();
                            storage.putIfAbsent(key, Lists.newArrayList());
                            if (Objects.nonNull(storage.get(key))) {
                                final InputMediaVideo mediaVideo = new InputMediaVideo(video.getFileId(), r.getCaption());
                                storage.get(key).add(mediaVideo);
                            }
                        }
                    }));
        }


        if (!CollectionUtils.isEmpty(storage)) {
            storage.values()
                    .stream()
                    .filter(f -> f.size() == 3)
                    .map(inputMedia -> {
                        final SendMediaGroup sendMediaGroup = new SendMediaGroup();
                        sendMediaGroup.setChatId(-1001297182819L);
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
            if (update.getMessage().hasText()) {
                final String text = update.getMessage().getText();
                SendMessage message = processMessage(update.getMessage().getChatId(), text);
                try {
                    execute(message); // Call method to send the message
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.getMessage().hasPhoto()) {
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

    private SendMessage processMessage(Long chatId, String text) {
        SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                .setChatId(chatId)
                .setText(text);

        if (text.equalsIgnoreCase("/start") || text.equalsIgnoreCase("Вернуться назад")) {
            message.setText("Привет! Ну что займемся работой?");
            message.setReplyMarkup(startMenuKeyboard());
        } else if (text.equalsIgnoreCase("Scraping channels")) {
            message.setText("Выбери канал");
            message.setReplyMarkup(chanelMenuKeyboard());
        } else if (Objects.nonNull(chanels.get(text))) {
            final TLChannel tlChannel = chanels.get(text);
            choosedChanel.set(tlChannel);
            message.setText("Выбери действие");
            message.setReplyMarkup(choosedChanelMenuKeyboard());
        } else if (text.equalsIgnoreCase("Get random story")) {
            getStory();
        }

        return message;
    }

    private void getStory() {
        final TLChannel tlChannel = choosedChanel.get();
        TLInputChannel channel = new TLInputChannel();
        channel.setAccessHash(tlChannel.getAccessHash());
        channel.setChannelId(tlChannel.getId());
        final Map<Integer, List<TLMessage>> messages = botService.getMessages(channel, 400);

        final ArrayList<Integer> list = new ArrayList<>(messages.keySet());
        Collections.shuffle(list);

        final List<TLMessage> tlMessages = messages.get(list.get(0));


        botService.sendCrappedMessagesToBot(botService.getDataBotPeer(), tlMessages);
        int a = 2;
    }

    private ReplyKeyboard choosedChanelMenuKeyboard() {
        final ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("Get random story"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(new KeyboardButton("Scraping channels"));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    private ReplyKeyboardMarkup chanelMenuKeyboard() {
        final ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        final TLAbsDialogs dialogs = botService.getDialogs();
        List<KeyboardRow> keyboard = new ArrayList<>();

        List<String> temp = Lists.newArrayList();
        dialogs
                .getChats()
                .stream()
                .filter(f -> f instanceof TLChannel)
                .map(f -> (TLChannel) f)
                .forEach(f -> {
                    chanels.put(f.getTitle(), f);
                    if (checkChanel(keyboard, temp, f)) {
                        temp.add(f.getTitle());
                        if (temp.size() == 2) {
                            final KeyboardRow keyboardButtons = new KeyboardRow();
                            keyboardButtons.addAll(temp);
                            keyboard.add(keyboardButtons);
                            temp.clear();
                        }
                    }
                });

        if (!CollectionUtils.isEmpty(temp)) {
            final KeyboardRow keyboardButtons = new KeyboardRow();
            keyboardButtons.addAll(temp);
            keyboard.add(keyboardButtons);
            temp.clear();
        }

        final KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add("Вернуться назад");
        keyboard.add(keyboardButtons);

        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    private Boolean checkChanel(List<KeyboardRow> keyboard, List<String> temp, TLChannel f) {

        return f.getTitle().toLowerCase().contains("фильмы")
                || f.getTitle().toLowerCase().contains("movie")
                || f.getTitle().toLowerCase().contains("film")
                || f.getTitle().toLowerCase().contains("hd")
                || f.getTitle().toLowerCase().contains("фильм");

    }

    private ReplyKeyboardMarkup startMenuKeyboard() {
        final ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("Scraping channels"));
        keyboardFirstRow.add(new KeyboardButton("Clear history"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(new KeyboardButton("Statistics"));
        keyboardSecondRow.add(new KeyboardButton("Test"));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        // TODO
        return "Kudr9tovbot";
    }

    @Override
    public String getBotToken() {
        // TODO
        return "1107620097:AAFWM3ajm6lz1cIT1ET00smqLVHGemcGyko";
    }

}
