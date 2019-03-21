package org.erachain.gui.items.templates;

import org.erachain.controller.Controller;
import org.erachain.core.item.templates.TemplateCls;
import org.erachain.utils.ObserverMessage;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("serial")
public class ComboBoxModelItemsTemplates extends DefaultComboBoxModel<TemplateCls> implements Observer {
    Lock lock = new ReentrantLock();

    public ComboBoxModelItemsTemplates() {
        Controller.getInstance().addWalletObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            if (lock.tryLock()) {
                try {
                    this.syncUpdate(o, arg);
                } finally {
                    lock.unlock();
                }
            }

        } catch (Exception e) {
            //GUI ERROR
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void syncUpdate(Observable o, Object arg) {
        ObserverMessage message = (ObserverMessage) arg;

        //CHECK IF LIST UPDATED
        if (message.getType() == ObserverMessage.LIST_TEMPLATE_FAVORITES_TYPE) {
            //GET SELECTED ITEM
            TemplateCls selected = (TemplateCls) this.getSelectedItem();

            //EMPTY LIST
            this.removeAllElements();

            //INSERT ALL ACCOUNTS
            Set<Long> keys = (Set<Long>) message.getValue();
            List<TemplateCls> templates = new ArrayList<TemplateCls>();
            for (Long key : keys) {
				/* key 0 - need as EMPTY
				if(key==0) {
					templates.add(null);
					continue;
				}
				*/

                //GET PLATE
                TemplateCls template = Controller.getInstance().getItemTemplate(key);
                templates.add(template);

                //ADD
                this.addElement(template);
            }

            //RESET SELECTED ITEM
            if (this.getIndexOf(selected) != -1) {
                for (TemplateCls template : templates) {
                    if (template.getKey() == selected.getKey()) {
                        this.setSelectedItem(template);
                        return;
                    }
                }
            }
        }
    }
}
