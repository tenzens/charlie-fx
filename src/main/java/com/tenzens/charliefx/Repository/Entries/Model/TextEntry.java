package com.tenzens.charliefx.Repository.Entries.Model;

import com.tenzens.charliefx.Repository.Model.Entry;
import com.tenzens.charliefx.Repository.Model.EntryBody;
import com.tenzens.charliefx.Repository.Model.TextContent;

import java.util.UUID;

public class TextEntry extends ClientEntry {
    public TextEntry(long id, Entry entry, boolean isLocal, String deliveryStatus) {
        super(id, entry, isLocal, deliveryStatus);
    }

    public static TextEntry createNewTextEntry(String deviceId, String title, String text, String color, String accentColor, boolean pinned) {
        EntryBody body = new EntryBody(
                color,
                accentColor,
                pinned,
                new TextContent(
                        title,
                        text
                )
        );

        Entry entry = new Entry(
                UUID.randomUUID().toString(),
                deviceId,
                System.currentTimeMillis(),
                body
        );

        return new TextEntry(-1, entry, true, DeliveryStatus.UNDELIVERED);
    }

    public TextContent getContent() {
        return (TextContent) entry.getBody().getContent();
    }
}
