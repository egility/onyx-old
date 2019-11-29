/*
 * Copyright (c) Mike Brickman 2014-2017
 */

/**
 * Created by mbrickman on 18/09/16.
 */

var dateFormat = require("./date_format.js");

var configFile = "/data/kotlin/config.json";
var siteName = "elephant";

process.argv.forEach(function (val, index, array) {
    console.log("Plaza-debug argument: " + val);
    var parts = val.split("=");
    if (parts[0] == "--config") {
        configFile = parts[1]
    }
    if (parts[0] == "--site") {
        siteName = parts[1]
        console.log("Plaza-debug siteName: " + siteName);
    }
});


var config = require(configFile);
var site = "";


function setSite() {
    for (index in config.sites) {
        console.log(config.sites[index].name + "  " + siteName);
        if (config.sites[index].name == siteName) {
            site = config.sites[index]
            console.log("Matched! API: " + site.api);
        }
    }
    return undefined
}

function dateNumber(date) {
    var base = new Date(date);
    return base.getFullYear() * 10000 + base.getMonth() * 100 + base.getDate();
}

function getConfig(key) {
    return site[key];
}


var dateRange = function (dateStart, dateEnd, noMonth, short) {
    var pattern="ddd d mmm";
    if (noMonth!=undefined && noMonth) {
        pattern="ddd d"
    }
    if (short!=undefined && short) {
        pattern="d"
    }
    var start = new Date(dateStart);
    var end = new Date(dateEnd);
    var base = new Date(0);
    if (dateStart == dateEnd || end <= base) {
        return dateFormat.dateFormat(dateStart, pattern)
    } else if (start.getMonth() != end.getMonth() && noMonth!=undefined && noMonth && false) {
        return dateFormat.dateFormat(dateStart, pattern) + " - " + dateFormat.dateFormat(dateEnd, pattern + " mmm")
    } else {
        return dateFormat.dateFormat(dateStart, pattern) + " - " + dateFormat.dateFormat(dateEnd, pattern)
    }
};

var toMoney = function (amount) {
    if (amount<0) {
        return "(£" + (-amount / 100).toFixed(2).replace(/(\d)(?=(\d{3})+\.)/g, '$1,') + ")";
    } else {
        return "£" + (amount / 100).toFixed(2).replace(/(\d)(?=(\d{3})+\.)/g, '$1,');
    }
};

var columnNameToLabel = function (columnName) {
    if (columnName == "house") {
        return "House Name or Number"
    }
    var spaced = columnName.replace(/([A-Z])/g, " $1");
    return spaced.charAt(0).toUpperCase() + spaced.slice(1); // capitalize the first letter - as an example.
};

var beforeToday = function (date) {
    return dateNumber(date) < dateNumber(new Date())
};

var isOrBeforeToday = function (date) {
    return dateNumber(date) <= dateNumber(new Date())
};

var afterToday = function (date) {
    return dateNumber(date) > dateNumber(new Date())
};

var isToday = function (date) {
    return dateNumber(date) == dateNumber(new Date())
};

var toJson = function(api) {
    var json = JSON.stringify(api);
    return json
};

var codeToText = function (code, codes) {
    for (var index in codes) {
        if (codes[index].value == code) {
            return codes[index].description
        }
    }
    return "Unknown"

};

var calcHandlingFee = function (credit, pence) {
    var rate = credit ? 0.0145 : 0.0078;
    var fixed = 2;
    var fee = Math.round((pence * rate) + fixed);
    return fee
};

var calcStripeFee = function (pence) {
    var rate = 0.014;
    var fixed = 20;
    var fee = Math.round((pence * rate) + fixed);
    return fee
};

var commaAppend = function (str, append) {
    if (str=="") {
        return append
    } else {
        return str + ", " + append
    }
};

module.exports = {
    dateRange: dateRange,
    columnNameToLabel: columnNameToLabel,
    toMoney: toMoney,
    setSite: setSite,
    getConfig: getConfig,
    beforeToday: beforeToday,
    isOrBeforeToday: isOrBeforeToday,
    afterToday: afterToday,
    isToday: isToday,
    toJson: toJson,
    codeToText: codeToText,
    calcHandlingFee: calcHandlingFee,
    commaAppend: commaAppend
};

