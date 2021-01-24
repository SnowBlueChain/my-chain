function statuses(data){
    var output = '';

    if(data.hasOwnProperty('error'))
    {
        return '<h2>' + data.error + '</h2>';
    }

    var notDisplayPages = data.notDisplayPages;
    var numberShiftDelta = data.numberOfRepresentsItemsOnPage;
    //Отображение последнего блока
    output += lastBlock(data.lastBlock);
    var start = data.start;

    if (!notDisplayPages) {
        //Отображение компонента страниц(вверху)
        //output += pagesComponentBeauty(start, data.Label_Statuses, data.numberLast, numberShiftDelta, 'start');
        output += pagesComponent2(data);
    }
    output += '<table width="1280" border=0><tr><td align=left><br>';
    output += '<table width=80% BORDER=0 cellpadding=10 cellspacing=0 class="table table-striped" style="border: 1px solid #ddd;">';
    output += '<thead><tr><td><b>'+ data.Label_table_key  +': ' +
        data.Label_table_name + '</b></td><td><b>' + data.Label_table_description +
        '</b></td><td><b>' + data.Label_table_creator + '</b></td></tr></thead>';

    //Отображение таблицы элементов статусов
    for (var i in data.pageItems) {
        var item = data.pageItems[i];
        output += '<tr><td><a href="?status=' + item.key + get_lang() + '">' + item.key + ': ';
        if (item.icon.length > 0)
            output += '<img src="data:image/gif;base64,' + item.icon + '" style="width:2em;" /> ';
        output += '<b>' + escapeHtml(item.name) + '</b></a></td>';
        output += '<td>' + escapeHtml(item.description.substr(0, 100)) + '</td>';

        output += '<td><a href=?address=' + item.owner + get_lang() + '>';
        if (item.hasOwnProperty('person'))
            output += '[' + item.person_key + ']' + escapeHtml(item.person);
        else
            output += item.owner;
        output += '</a></td></tr>';
    }
    if (!notDisplayPages) {
        //Отображение ссылки предыдущая
        output += '</table></td></tr></table>';
        //Отображение компонента страниц(снизу)
        //output += pagesComponentBeauty(start, data.Label_Statuses, data.numberLast, numberShiftDelta, 'start');
        output += pagesComponent2(data);
    }

    return output;
}

function status(data, forPrint) {

    var output = '';

    if (!forPrint)
        output += lastBlock(data.lastBlock);

    if (!data.hasOwnProperty('item')) {
        output += '<h2>Not found</h2>';
        return output;
    }

    if (data.hasOwnProperty('error')) {
        output += '<br><h5>' + data.error + '</h5>';

        return output;
    }

    output += '<table id=blocks BORDER=0 cellpadding=15 cellspacing=0 width="1300">';
    output += '<tr><td align=left>';
    output += '<table><tr style="vertical-align:top">';

    var item = data.item;
    ////// HEAD
    output += itemHead(item, forPrint);

    //////// BODY
    output += '<p style="font-size:1.3em">';

    if (item.unique) {
        output += '<b>' + item.Label_unique_state + '</b><br>'
    } else {
        output += '<b>' + item.Label_multi_states + '</b><br>'
    }

    output += '</p>';

    //// FOOT
    output += itemFoot(item, forPrint);

    return output;
}
