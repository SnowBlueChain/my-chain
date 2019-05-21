function makePageUri(page, linkName) {
    // parse url
    var urlParams;
    var match,
        pl = /\+/g,  // Regex for replacing addition symbol with a space
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s) {
            return decodeURIComponent(s.replace(pl, " "));
        },
        query = window.location.search.substring(1);

    urlParams = {};
    while (match = search.exec(query))
        urlParams[decode(match[1])] = decode(match[2]);

    urlParams[linkName] = page;
    var uri = '';

    for (var paramKey in urlParams) {
        if (uri === '') {
            uri += '?';
        } else {
            uri += '&';
        }

        uri += paramKey + '=' + encodeURIComponent(urlParams[paramKey]);
    }

    return uri;
}

function pagesComponent(data) {
    var output = '';

    if (data.pageCount > 1) {
        output += 'Pages: ';
        for (var page = 1; page <= data.pageCount; page++) {
            if (page == data.pageNumber) {
                output += '<b>' + page + '</b>&nbsp;';
            } else {
                output += '<a href="' + makePageUri(page, 'page') + '">' + page + '</a>&nbsp;';
            }
        }
    }

    return output;
}

function pagesComponent2(data) {
    var output = '';

    var listSize = data.listSize;
    var pageSize = data.pageSize;
    var start = data.start;

    if (data.hasOwnProperty('start')) {
        start = data.start;
    } else {
        var start = listSize;
    }

    if (start == 0)
        start = 1;

    if (1 != start)
        output += '<a class="button ll-blue-bgc" href="' + makePageUri(1, 'start') + '"><b>' + '1</b></a>';

    if (start > pageSize * 10) {

        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start - pageSize * 10, 'start') + '"><b>' + (start - pageSize * 10) + '</b></a>';
        //output += (start - pageSize * 10) + ' --- ';
    }
    if (start > pageSize + 1) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start - pageSize, 'start') + '"><b>' + (start - pageSize) + '</b></a>';
        //output += (start - pageSize) + ' - ';
    }

    output += '&emsp; <a class="button ll-blue-bgc active" href="' + makePageUri(start, 'start') + '"><b> ' + start + ' </b></a>';

    if (start + pageSize < listSize) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start + pageSize, 'start') + '"><b>' + (start + pageSize) + '</b></a>';
    }
    if (start + pageSize * 10 < listSize) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start + pageSize * 10, 'start') + '"><b>' + (start + pageSize * 10) + '</b></a>';
    }

    if (listSize != start)
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(listSize, 'start') + '"><b>' + listSize + '</b></a>';

    return output;
}

function pagesComponent3(data) {
    var output = '';

    var listSize = data.listSize;
    var pageSize = data.pageSize;
    var start = data.start;

    if (data.hasOwnProperty('start')) {
        start = data.start;
    } else {
        var start = listSize;
    }

    if (start == 0)
        start = 1;

    if (start == 1) {
        output += '<a class="button ll-blue-bgc active" href="' + makePageUri(1, 'start') + '"><b>' + 'last</b></a>';
    } else {
        output += '<a class="button ll-blue-bgc" href="' + makePageUri(1, 'start') + '"><b>' + 'last</b></a>';
    }

    if (start > pageSize * 10) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start - pageSize * 10, 'start') + '"><b>' + '&lt;&lt;' + '</b></a>';
    }

    if (start > pageSize + 1) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start - pageSize, 'start') + '"><b>' + '&lt;' + '</b></a>';
    }

    if (start != 1 && start < listSize - pageSize) {
        output += '&emsp; <a class="button ll-blue-bgc active" href="' + makePageUri(start, 'start') + '"><b> -' + start + ' </b></a>';
    }

    if (start + pageSize < listSize) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start + pageSize, 'start') + '"><b>' + '&gt;' + '</b></a>';
    }
    if (start + pageSize * 10 < listSize) {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(start + pageSize * 10, 'start') + '"><b>' + '&gt;&gt;' + '</b></a>';
    }

    if (start >= listSize - pageSize) {
        output += '&emsp; <a class="button ll-blue-bgc active" href="' + makePageUri(listSize - pageSize, 'start') + '"><b>' + '-' + (listSize - pageSize) + '</b></a>';
    } else {
        output += '&emsp; <a class="button ll-blue-bgc" href="' + makePageUri(listSize - pageSize, 'start') + '"><b>' + '-' + (listSize - pageSize) + '</b></a>';
    }

    return output;
}


function pagesComponentMixed(data) {
    if (data.pageCount < 10) {
        return pagesComponent(data);
    } else if (data.pageCount >= 10) {
        return pagesComplexComponent(data);
    }
}

function pagesComplexComponent(data) {
    var output = '';
    if (data.pageCount > 1) {
        output += 'Pages: ';
        //В начале пагинация первых 3ех
        for (var page = 1; page <= 3; page++) {
            if (page == data.pageNumber) {
                output += '<b>' + page + '</b>&nbsp;';
            } else {
                output += '<a href="' + makePageUri(page, 'page') + '">' + page + '</a>&nbsp;';
            }
        }
        output += '...';
        //пагинация от текущего

        for (var page = data.pageNumber - 2; page <= data.pageNumber + 2; page++) {
            if (page > 2 && page < data.pageCount - 2) {
                if (page == data.pageNumber) {
                    output += '<b>' + page + '</b>&nbsp;';
                } else {
                    output += '<a href="' + makePageUri(page, 'page') + '">' + page + '</a>&nbsp;';
                }
            }
        }
        output += '...';
        //В конце пагинация последних 3ех
        for (var page = data.pageCount - 2; page <= data.pageCount; page++) {
            if (page == data.pageNumber) {
                output += '<b>' + page + '</b>&nbsp;';
            } else {
                output += '<a href="' + makePageUri(page, 'page') + '">' + page + '</a>&nbsp;';
            }
        }
    }
    return output;
}


function pageCreation(from, to, step, restriction, start, linkName) {
    var output = '';
    for (var page = from; page > to; page -= step) {
        if (page >= step) {
            if (page >= 1 && page <= restriction) {
                if (page === start) {
                    output += '<b>' + page + '</b>&nbsp;';
                    continue;
                }
                output += '<a href="' + makePageUri(page, linkName) + '">' + page + '</a>&nbsp;';

            }
        }
    }
    return output;
}

function pagesComponentBeauty(start, label, numberLast, step, linkName) {
    var output = '';
    var delta = 5;
    var numberPages = 3;
    if (start >= 1) {
        output += label + ':';
        output += pageCreation(numberLast, numberLast - (numberPages - 2) * step + 1, step, numberLast, start, linkName);
        output += '...';
        output += pageCreation(start + step * delta, (start - step * delta), step, numberLast - 1, start, linkName);
        output += '...';
        output += pageCreation(numberPages * step, 1, step, numberLast, start, linkName);
    }
    return output;
}

function transactions_Table(data) {
    console.log("data=")
    console.log(data)
    var output = data.Transactions.label_transactions_table + ':<br>';
    //output += pagesComponentMixed(data);
    output += pagesComponent3(data);

    output += '<table id="transactions" id=accounts BORDER=0 cellpadding=15 cellspacing=0 width="800" ' +
        ' class="table table-striped" style="border: 1px solid #ddd; word-wrap: break-word;" >';

    output += '<tr bgcolor="f1f1f1"><td><b>' + data.Transactions.label_block + '<td><b>' +
        data.Transactions.label_signature + '<td><b>' + data.Transactions.label_type_transaction + '<td><b>' +
        data.Transactions.label_amount_key + '<td><b>' + data.Transactions.label_date + '<td><b>' +
        data.Transactions.label_atside + '<td><b>' + data.Transactions.label_size + '<td><b>' +
        data.Transactions.label_fee + '<td><b>' + data.Transactions.label_confirmations + '</tr>';
    for (key in data.Transactions.transactions) {
        output += '<tr><td><a href ="?tx=' + data.Transactions.transactions[key].block + '-'
            + data.Transactions.transactions[key].seqNo + get_lang() + '">' + data.Transactions.transactions[key].block + '-' +
            data.Transactions.transactions[key].seqNo + '</a><td><a href="?tx=' +
            data.Transactions.transactions[key].signature + get_lang() + '" title = "' +
            data.Transactions.transactions[key].signature + get_lang() + '">' +
            data.Transactions.transactions[key].signature.slice(0, 11) + '...</a><td>'
        if (data.Transactions.transactions[key].type !== 'forging') {
            output += '<a href="?tx=' + data.Transactions.transactions[key].signature + get_lang() + '">'
        }
        output += data.Transactions.transactions[key].type +
            '</a><td>' + data.Transactions.transactions[key].amount_key + '<td>' + data.Transactions.transactions[key].date;
        output += '<td><a href ="?addr=' + data.Transactions.transactions[key].creator_addr + get_lang() + '">' +
            data.Transactions.transactions[key].creator + '</a>';
        output += '<td>' + data.Transactions.transactions[key].size + '<td>' +
            data.Transactions.transactions[key].fee + '<td>' + data.Transactions.transactions[key].confirmations + '</td></tr>';

    }
    output += '</table></td></tr></table>';
    //output += pagesComponentMixed(data);
    output += pagesComponent3(data);


    return output;

}