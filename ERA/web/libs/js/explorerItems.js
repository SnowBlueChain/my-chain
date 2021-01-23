function itemHead(item, forPrint) {

    var output = '';
    var type = item.item_type;

    if (item.image) {
        output += '<td><img src="data:image/gif;base64,' + item.image + '" width = "350" /></td><td style ="width: 70%; padding-left:20px">';
        output += '<br>';
    }

    output += '<h3 style="display:inline;">';

    if (!forPrint)
        output += '<a href="?' + type + '=' + item.key + get_lang() + '">';

    if (item.icon)
        output += ' <img src="data:image/gif;base64,' + item.icon + '" style="width:50px;" /> ';

    output += item.name;

    if (!forPrint)
        output += '</a>';

    output += '</h3><h4>';

    if (forPrint)
        output += item.Label_Number + ':<b> ' + item.key + '</b>';
    else
        output += '[ <input id="key1" name="' + type + '" size="8" type="text" value="' + item.key + '" class="" style="font-size: 1em;"'
                       + ' onkeydown="if (event.keyCode == 13) buttonSearch(this)"> ]';

    output += ', &nbsp' + item.Label_DateIssue + ':<b> ' + convertTimestamp(item.blk_timestamp, true) + '</b></h4>';

    output += '<h4>' + item.Label_Owner + ': ';
    if (item.owner_person) {
        if (forPrint)
            output += '<b>' + item.owner_person + ' (' + item.creator + ')</b></h4>';
        else
            output += '<a href ="?address=' + item.owner + get_lang() + '"><b> ' + item.owner_person + '</b></a></h4>';
    } else {
        if (forPrint)
            output += '<b>' + item.owner + '</b></h4>';
        else
            output += '<a href ="?address=' + item.owner + get_lang() + '"><b> ' + item.owner + '</b></a></h4>';
    }

    if (item.hasOwnProperty('seqNo')) {
        output +=  '<h4>' + item.Label_TXIssue;
        var creator;
        if (item.owner_person) {
            if (forPrint)
                creator = '<b>' + item.owner_person + ' (' + item.creator + ')</b></h4>';
            else
                creator = '<a href ="?address=' + item.owner + get_lang() + '"><b> ' + item.owner_person + '</b></a></h4>';
        } else {
            if (forPrint)
                creator = '<b>' + item.owner + '</b></h4>';
            else
                creator = '<a href ="?address=' + item.owner + get_lang() + '"><b> ' + item.owner + '</b></a></h4>';
        }
        if (forPrint) {
            output += ': <b> ' + item.seqNo + '</b></h4>';
            output += '&nbsp&nbsp&nbsp&nbsp' + item.Label_Signature + ':<b> ' + item.reference + '</b><br>';
            output += '&nbsp&nbsp&nbsp&nbsp' + item.Label_TXCreator + ':<b> ' + creator + '</b><br>';
        } else {
            output += ': <a href=?tx=' + item.seqNo + get_lang() + ' class="button ll-blue-bgc"><b>' + item.seqNo + '</b></a></h4>';
            output += '&nbsp&nbsp&nbsp&nbsp' + item.Label_Signature + ':<b> ' + item.reference + '</b><br>';
            output += '&nbsp&nbsp&nbsp&nbsp' + item.Label_TXCreator + ':<b> ' + creator + '</b><br>';

            output += '<a href=?q=' + item.charKey + get_lang() + '&search=transactions class="button ll-blue-bgc"><b>' + item.Label_Actions + '</b></a>';
        }
    }
    if (!forPrint) {
        output += ' &nbsp&nbsp<a href=../api'+ type + '/raw/' + item.key + ' class="button ll-blue-bgc"><b>' + item.Label_RAW + '</b></a>';
        output += ' &nbsp&nbsp<a href=?'+ type + '=' + item.key + get_lang() + '&print class="button ll-blue-bgc"><b>' + item.Label_Print + '</b></a></h4>';
    }

    return output;

}

function itemFoot(item, forPrint) {
    var type = item.item_type;

    var output = '';
    if (item.description)
        output += '<h3>' + item.Label_Description + '</h3><br>' + fformat(item.description);

    return output;
}