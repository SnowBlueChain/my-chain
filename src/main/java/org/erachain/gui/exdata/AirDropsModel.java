package org.erachain.gui.exdata;

import org.erachain.core.account.Account;
import org.erachain.gui.transaction.OnDealClick;
import org.erachain.lang.Lang;
import org.mapdb.Fun;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Vector;

public class AirDropsModel extends DefaultTableModel {

    public static String lastError;
    static Vector<Object> headVector = new Vector<Object>(8) {{
        add(Lang.T("No."));
        add(Lang.T("Account"));
        add(Lang.T("Error"));
    }};

    public AirDropsModel() {
        super(new Vector(), headVector);
        lastError = null;
        addRow(new Object[]{0, "", ""});
    }

    public AirDropsModel(String[] addresses) {
        super(setRows(addresses), headVector);
    }

    public AirDropsModel(List<Fun.Tuple2<Account, Fun.Tuple2<Integer, String>>> accruals, boolean onlyErrors) {
        super(setRows(accruals, onlyErrors), headVector);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 1;
    }

    static Vector setRows(String[] addresses) {
        lastError = null;

        int count = 0;
        Vector<Vector> data = new Vector();

        Vector<Object> rowVector;
        Fun.Tuple2<Account, String> result;
        for (String item : addresses) {
            result = Account.tryMakeAccount(item);
            rowVector = new Vector<Object>(8);
            rowVector.addElement(++count);
            if (result.a == null) {
                rowVector.addElement(item);
                rowVector.addElement(result.b);
                lastError = result.b;
            } else {
                rowVector.addElement(result.a.getPersonAsString());
                rowVector.addElement("");
            }

            data.add(rowVector);
        }

        return data;
    }

    static Vector setRows(List<Fun.Tuple2<Account, Fun.Tuple2<Integer, String>>> accruals, boolean onlyErrors) {
        lastError = null;

        int count = 0;
        Vector data = new Vector();

        Vector<Object> rowVector;

        for (Fun.Tuple2<Account, Fun.Tuple2<Integer, String>> item : accruals) {

            if (onlyErrors && item.b == null)
                continue;

            rowVector = new Vector<Object>(8);
            rowVector.addElement(++count);
            if (item.b == null) {
                rowVector.addElement("");
            } else {
                rowVector.addElement(Lang.T(OnDealClick.resultMess(item.b.a)) + (item.b.b == null ? "" : " - " + item.b.b));
            }

            data.add(rowVector);
        }

        return data;
    }

}

