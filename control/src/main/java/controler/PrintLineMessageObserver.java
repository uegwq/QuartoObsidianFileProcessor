package controler;

import model.MessageObserver;

public class PrintLineMessageObserver extends MessageObserver {

    @Override
    public void notify(String updateText) {
        System.out.println(updateText);
    }
}
