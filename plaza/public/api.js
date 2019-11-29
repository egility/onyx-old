/*
 * Copyright (c) Mike Brickman 2014-2017
 */

/**
 * Created by mbrickman on 14/10/16.
 */

var _datePattern;

function datePattern() {
    if (_datePattern == undefined) {
        var pattern = new Date(Date.UTC(2018, 5, 15, 12, 0, 0)).toLocaleDateString();
        _datePattern = pattern.replace("2018", "yyyy").replace("06", "mm").replace("15", "dd")
    }
    return _datePattern
}

function bindForm(formId, apiUrl, apiData) {
    jQuery(document).ready(function () {
        jQuery("#" + formId).submit(function (event) {
            event.preventDefault();
            doSubmit(formId, apiUrl, apiData)
        });
        bindInputs(formId, apiData);
    });
    apiData.formId = formId;
    return apiData
}

function addResponseData(url, apiResponse) {
    var query = "";
    if (apiResponse && apiResponse.control && apiResponse.control.data) {
        for (var item in apiResponse.control.data) {
            if (query != "") {
                query += "&"
            }
            query += item + "=" + apiResponse.control.data[item]
        }
    }
    if (query != "") {
        return url + "?" + query
    } else {
        return url
    }
}

function doSubmit(formId, apiUrl, apiData) {
    showLoading();
    unBindInputs(formId, apiData);
    apiData.host = location.origin;

    putJSON(apiUrl, apiData,
        function (apiResponse, status, jqXHR) {
            if (status == "success") {
                if (apiResponse && apiResponse.control && apiResponse.control.error && apiResponse.control.error != 0) {
                    hideLoading();
                    haveError(formId, apiResponse)
                } else {
                    var select = apiResponse && apiResponse.control && apiResponse.control.action ? apiResponse.control.action : "done";
                    var doneUrl = location.origin + location.pathname + "/" + select + window.location.search

                    if (select == "authenticated") {
                        window.location.href = addResponseData("/index_authenticated", apiResponse)
                    } else if (select == "switched") {
                        window.location.href = addResponseData("/index_switched", apiResponse)
                    } else if (select == "reverted") {
                        window.location.href = addResponseData("/index_reverted", apiResponse)
                    } else if (select == "error") {
                        alert(apiResponse.control.message);
                        hideLoading()
                    } else {
                        window.location.href = addResponseData(doneUrl, apiResponse)
                    }
                }
            } else {
                hideLoading();
                alert("Something has gone wrong (" + status + "). Please try again later")
            }
        },
        function (jqXHR, status, errorThrown) {
            hideLoading();
            alert("There maybe a network issue (" + status + ":" + errorThrown + "). Please try again later")
        });
    event.preventDefault();
}

function fetch(uri, callback) {
    showLoading();
    getJSON(uri, function (apiResponse, status, jqXHR) {
        if (status == "success") {
            if (apiResponse && apiResponse.control && apiResponse.control.error && apiResponse.control.error != 0) {
                hideLoading();
                callback({})
            } else {
                hideLoading();
                callback(apiResponse.data);
            }
        } else {
            hideLoading();
            alert("Something has gone wrong (" + status + "). Please try again later")
        }
    }, function (jqXHR, status, errorThrown) {
        hideLoading();
        alert("There maybe a network issue (" + status + ":" + errorThrown + "). Please try again later")
    });

}

function haveError(formId, apiResponse) {
    jQuery("#" + formId + " .has-danger").removeClass("has-danger");
    jQuery("#" + formId + " .form-control-feedback").remove();
    jQuery("#" + formId + " .form-error").remove();
    for (i in apiResponse.dataErrors) {
        var dataError = apiResponse.dataErrors[i];
        jQuery("#" + formId + " [data-api-bind = '" + dataError.path + "']").each(function (index) {
            jQuery(this).closest("#" + formId + " .form-group").addClass("has-danger");
            jQuery(this).after('<span  class="form-control-feedback">' + dataError.message + '</span>')
        });
    }
    jQuery("#" + formId).prepend('<div  class="form-error"><i class="fa fa-warning"></i>&nbsp;' + apiResponse.control.comment + '</div>')

    $(window).scrollTop(0);

}

function bindInputs(formId, api) {
    jQuery("#" + formId + " [data-api-bind]").each(function (index) {
        if (this.getAttribute("data-api-bind") != "") {
            if (this.getAttribute("type") == "checkbox") {
                if (this.hasAttribute("data-api-inverted")) {
                    this.checked = !getValue(api, this.getAttribute("data-api-bind"));
                } else {
                    this.checked = getValue(api, this.getAttribute("data-api-bind"));
                }
            } else if (this.getAttribute("type") == "radio") {
                this.checked = getValue(api, this.getAttribute("data-api-bind")) == this.id;
            } else {
                var value = getValue(api, this.getAttribute("data-api-bind"));
                if (this.getAttribute("data-type") == "date") {
                    this.setAttribute("placeholder", datePattern());
                    var msec = Date.parse(value);
                    if (value == undefined || value == "" || isNaN(msec) || msec == 0) {
                        this.value = "";
                    } else {
                        this.value = new Date(msec).toLocaleDateString();
                    }
                } else {
                    this.value = value != undefined ? value : "";
                }
            }
        }
    });
}

function unBindInputs(formId, api) {
    var bindings = [];
    api.datePattern = datePattern;
    jQuery("#" + formId + " [data-api-bind]").each(function (index) {
        var bind = this.getAttribute("data-api-bind");
        if (bind != "") {
            bindings.push(bind)
            if (this.getAttribute("type") == "checkbox") {
                if (this.hasAttribute("data-api-inverted")) {
                    setValue(api, bind, !this.checked);
                } else {
                    setValue(api, bind, this.checked);
                }
            } else if (this.getAttribute("type") == "radio") {
                if (this.checked) {
                    setValue(api, bind, this.id);
                    setValue(api, bind, this.id);
                }
            } else {
                var num = Number(this.value);
                switch (this.type) {
                    case "hidden":
                        var num = Number(this.value);
                        if (!isNaN(num)) {
                            setValue(api, bind, num);
                        } else {
                            setValue(api, bind, this.value);
                        }
                        break;
                    case "number":
                        setValue(api, bind, this.valueAsNumber);
                        break;
                    default:
                        setValue(api, bind, this.value);
                }
            }
        }
    });
    setValue(api, "bindings", bindings);
}

function putJSON(url, api, success, error) {
    jQuery.support.cors = true;
    return jQuery.ajax({
        method: "PUT",
        url: url,
        data: JSON.stringify(api),
        success: success,
        error: error,
        dataType: "json",
        contentType: "application/json",
        processData: false
    });
}

function getJSON(url, success, error) {
    jQuery.support.cors = true;
    return jQuery.ajax({
        method: "GET",
        url: url,
        data: {},
        success: success,
        error: error,
        dataType: "json",
        contentType: "application/json",
        processData: false
    });
}


function putXls(url, data, success, error) {
    jQuery.support.cors = true;
    return jQuery.ajax({
        method: "PUT",
        url: url,
        data: data,
        success: success,
        error: error,
        dataType: "json",
        contentType: "application/xls",
        processData: false
    });
}

function uploadFile(event, url) {
    showLoading();
    var input = event.target;

    var reader = new FileReader();
    reader.onload = function () {
        putXls(url, reader.result, function (apiResponse, status, jqXHR) {
            if (status == "success") {
                if (apiResponse && apiResponse.control && apiResponse.control.error && apiResponse.control.error != 0) {
                    hideLoading();
                    haveError(formId, apiResponse)
                } else {
                    var action = apiResponse && apiResponse.control && apiResponse.control.action ? apiResponse.control.action : "done";
                    if (action == "popup") {
                        alert(apiResponse.control.message);
                        window.location.reload()
                    } else if (action == "error") {
                        alert(apiResponse.control.message);
                        hideLoading();
                    } else {
                        var doneUrl = location.origin + location.pathname + "/" + action;
                        window.location.href = addResponseData(doneUrl, apiResponse)
                    }
                }
            } else {
                hideLoading();
                alert("Unable to process this file (" + status + "). Please try again later")
            }
        }, function (jqXHR, status, errorThrown) {
            hideLoading();
            alert("Unable to process this file 2 (" + apiUrl + ":" + status + "). Please try again later")
        });
    };
    reader.readAsArrayBuffer(input.files[0]);
    input.value = null
}


function getValue(data, bind) {
    var parts = bind.split(".");
    var item = data;
    for (var i in parts) {
        if (item[parts[i]] == undefined) {
            return undefined
        } else {
            item = item[parts[i]];
        }
    }
    return item
}

function setValue(data, bind, value) {
    var parts = bind.split(".");
    var object = data;
    var selector = "";
    for (var i in parts) {
        if (selector != "") {
            if (object[selector] == undefined) {
                object[selector] = {}
            }
            object = object[selector]
        }
        selector = parts[i];
    }
    object[selector] = value;
}

function showLoading() {
    jQuery('#loading').css("visibility", "visible");
}

function hideLoading() {
    jQuery('#loading').css("visibility", "hidden");
}

function unCollapsecard(id) {
    /*
     if (jQuery('#' + id).hasClass("collapse")) {
     jQuery('#' + id).removeClass("collapse");
     } else {
     jQuery('#' + id).addClass("collapse");
     }
     */
    if (!jQuery('#' + id).hasClass("collapse")) {
        jQuery(".collapsible").addClass("collapse");
    } else {
        jQuery(".collapsible").addClass("collapse");
        jQuery('#' + id).removeClass("collapse");
    }
}

function toggleNavPanel() {
    if (jQuery('body').hasClass("show-nav-panel")) {
        jQuery('body').removeClass("show-nav-panel");
        jQuery('.nav-panel-toggle i').removeClass("fa-close");
        jQuery('.nav-panel-toggle i').addClass("fa-bars");
        jQuery('.nav-panel-toggle span').text("Menu")

    } else {
        jQuery('body').addClass("show-nav-panel");
        jQuery('.nav-panel-toggle i').removeClass("fa-bars");
        jQuery('.nav-panel-toggle i').addClass("fa-close");
        jQuery('.nav-panel-toggle span').text("Close")

    }
}


