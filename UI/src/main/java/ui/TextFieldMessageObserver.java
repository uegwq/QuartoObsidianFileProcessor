package ui;

import model.MessageObserver;

public class TextFieldMessageObserver extends MessageObserver {

    private final FileProcessorUi ui;

    public TextFieldMessageObserver(FileProcessorUi inputUi) {
        this.ui = inputUi;
    }

    @Override
    public void notify(String updateText) {
        ui.addTextToField(updateText);
    }
}
