package com.sapphire.microphone.model;


public class LastPreviewSize {
    private final int width;
    private final int height;

    public LastPreviewSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
