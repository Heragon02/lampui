package com.lampui.client;

/** Интерфейс, который вмешивается в ClickableWidget, чтобы хранить время последнего клика. */
public interface LampClickState {
    long lampui$getClickMs();
}
