package org.erachain.gui.exdata;

import org.erachain.core.account.Account;
import org.erachain.gui.transaction.OnDealClick;
import org.erachain.lang.Lang;
import org.mapdb.Fun;

import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;

public class AirDropsModel extends DefaultTableModel {

    static Vector<Object> headVector = new Vector<Object>(8) {{
        add(Lang.T("No."));
        add(Lang.T("Account"));
        add(Lang.T("Error"));
    }};

    public AirDropsModel() {
        super(new Vector(), headVector);
        addRow(new Object[]{0, "", ""});
    }

    public AirDropsModel(BigDecimal amount, String[] addresses) {
        super(setRows(amount, addresses), headVector);
    }

    public AirDropsModel(List<Fun.Tuple3<Account, BigDecimal, Fun.Tuple2<Integer, String>>> accruals, boolean onlyErrors) {
        super(setRows(accruals, onlyErrors), headVector);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 1;
    }

    static Vector setRows(BigDecimal amount, String[] addresses) {
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
            } else {
                rowVector.addElement(result.a.getPersonAsString());
                rowVector.addElement("");
            }

            data.add(rowVector);
        }

        return data;
    }

    static Vector setRows(List<Fun.Tuple3<Account, BigDecimal, Fun.Tuple2<Integer, String>>> accruals, boolean onlyErrors) {
        int count = 0;
        Vector data = new Vector();

        Vector<Object> rowVector;

        for (Fun.Tuple3<Account, BigDecimal, Fun.Tuple2<Integer, String>> item : accruals) {

            if (onlyErrors && item.c == null)
                continue;

            rowVector = new Vector<Object>(8);
            rowVector.addElement(++count);
            rowVector.addElement(item.a.getPersonAsString());
            rowVector.addElement(item.b.toPlainString());
            if (item.c == null) {
                rowVector.addElement("");
            } else {
                rowVector.addElement(Lang.T(OnDealClick.resultMess(item.c.a)) + (item.c.b == null ? "" : " - " + item.c.b));
            }

            data.add(rowVector);
        }

        return data;
    }

}

