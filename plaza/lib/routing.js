/*
 * Copyright (c) Mike Brickman 2014-2017
 */

var express = require('express');
var router = express.Router();
var request = require('request');
var utils = require('./utils');

var apiRoot = utils.getConfig("api");

function putApiData(apiPath, body, callback) {
    var uri = apiRoot + apiPath;
    console.log('API PUT ' + uri);
    request.put(
        {uri: uri, method: "PUT", json: true, body: body},
        function (error, response, json) {
            if (!error && response.statusCode === 200) {
                console.log(apiPath + " - success");
                callback(json);
            } else if (response) {
                console.log(apiPath + " - " + error + " (" + response.statusCode + ")");
                process.exit(1);
            } else {
                console.log(apiPath + " - " + error);
                process.exit(1);
            }
        }
    )
}

function getApiData(apiPath, timeout, callback) {
    var uri = apiRoot + apiPath;
    console.log('API GET ' + uri + " (" + timeout + ")" );
    request(
        {uri: uri, method: "GET", json: true, timeout: timeout},
        function (error, response, json) {
            if (!error && response.statusCode === 200) {
                console.log(apiPath + " - success");
                callback(json);
            } else if (response) {
                console.log(apiPath + " - " + error + " (" + response.statusCode + ")");
                process.exit(1);
            } else {
                console.log(apiPath + " - " + error);
                process.exit(1);
            }
        }
    )
}

function getAPI(apiTemplate, req, callback) {
    getApiData(substitute(apiTemplate, req), undefined, callback);
}

function isAuthenticated(req) {
    return req.session != undefined && req.session.data != undefined && req.session.data.authenticated != undefined && req.session.data.authenticated
}

function substitute(template, req) {
    var result = template;

    if (result.indexOf("$")) {
        var query = "";
        for (var key in req.query) {
            var value = req.query[key];
            if (query == "") {
                query = key + "=" + value
            } else {
                query = query + "&" + key + "=" + value
            }
        }
        result = result.replace("$", query)
    }
    // replace placeholders with actual data
    for (var param in req.params) {
        result = result.replace(":" + param + "/", req.params[param]+ "/");
        result = result.replace(":" + param + "?", req.params[param]+ "?");
        result = (result + "~").replace(":" + param + "~", req.params[param]+ "~").slice(0, -1);
    }
    for (var key in req.query) {
        var value = req.query[key];
        result = result.replace("~" + key, value)
    }
    if (req.session.data != undefined) {
        for (var item in req.session.data) {
            var value = req.session.data[item];
            result = result.replace("@" + item, value)
        }
    }
    return result;
}

function updateSessionFromObject(session, object) {
    for (item in object) {
        var value = object[item];
        // if value is a string, try to convert to the correct type
        if (typeof(value) === "string") {
            if (!isNaN(Number(value))) {
                session[item] = Number(value)
            } else if (value == "true") {
                session[item] = true
            } else if (value == "false") {
                session[item] = false
            } else if (value == "null") {
                session[item] = null
            } else {
                session[item] = value
            }
        } else {
            session[item] = value
        }
    }
}

function optionDef(options, name, def) {
    return options != undefined && options[name] != undefined ? options[name] : def;
}

function add(route, options) {

    if (options == undefined) {
        options = {};
    }
    if (options.render == undefined && !options.handover) {
        options.render = route
    }

    if (options.handover) {
        router.put(route, function (req, res, next) {
            var path = req.originalUrl.substr(options.handover.length);
            putApiData(path, req.body, function (apiData) {
                res.setHeader('Content-Type', 'application/json');
                res.end(JSON.stringify(apiData, null, 0));
            });
        });
    } else {
        router.get(route, function (req, res, next) {
            if (req.query.root && req.session.breadcrumbs != undefined) {
                req.session.breadcrumbs.length = 0;
            }
            if (optionDef(options, "session") == "clear" || req.session.data == undefined) {
                req.session.data = {}
            }
            var apiTemplate = optionDef(options, "api");
            if (apiTemplate == undefined) {
                apiTemplate = "/null";
            }
            if (!apiTemplate.includes("idCompetitor")) {
                apiTemplate += (apiTemplate.includes("?") ? "&" : "?") + "idCompetitor=@idCompetitor";
            }
            if (!apiTemplate.includes("idAccount")) {
                apiTemplate += (apiTemplate.includes("?") ? "&" : "?") + "idAccount=@idAccount";
            }
            if (!apiTemplate.includes("idCompetitorReal")) {
                apiTemplate += (apiTemplate.includes("?") ? "&" : "?") + "idCompetitorReal=@idCompetitorReal";
            }
            apiTemplate += (apiTemplate.includes("?") ? "&" : "?") + "access=@access";
            getAPI(apiTemplate, req, function (apiData) {
                // if this api returns a session object then add that to the session data
                if (apiData && apiData.control && apiData.control.session) {
                    updateSessionFromObject(req.session.data, apiData.control.session);
                }
                respond(options, req, res, apiData);
            });
        });
    }
}

function respond(options, req, res, apiData) {

    if (apiData.kind == "error") {
        console.log('error ' + apiData.error);
        render("system/apiFatal", req, res, apiData, apiData)
    } else {
        var redirectPath = optionDef(options, "redirect");
        var authenticatedPath = optionDef(options, "authenticated");
        if (apiData && apiData.control && apiData.control.action) {
            redirectPath = req._parsedUrl.pathname + "_" + apiData.control.action + "?" + req._parsedUrl.query;
        }
        if (isAuthenticated(req) && authenticatedPath != undefined) {
            authenticatedPath = substitute(authenticatedPath, req);
            console.log('redirect ' + authenticatedPath);
            res.redirect(authenticatedPath);
        } else if (redirectPath != undefined) {
            console.log('redirect ' + redirectPath);
            redirectPath = substitute(redirectPath, req);
            res.redirect(redirectPath);
        } else {
            var renderView = optionDef(options, "render", options.route);
            // remove leading slash from render if needed
            renderView = renderView=="/magazine/*" ? req.originalUrl.replace("/magazine", "/open/magazine") : renderView;
            renderView = renderView[0] == "/" ? renderView.substr(1) : renderView;
            console.log('render ' + renderView);
            render(renderView, req, res, apiData)
        }
    }
}

function render(renderView, req, res, apiData, fatal) {
    var resourceUri = apiData != undefined && apiData.control != undefined && apiData.control.resource != undefined ? "/api/resource/" + apiData.control.resource : "";
    var error = apiData != undefined && apiData.control != undefined ? apiData.control.error : 0;
    var comment = apiData != undefined && apiData.control != undefined ? apiData.control.comment : "";
    var data = apiData != undefined && apiData.data != undefined ? apiData.data : {};
    var session = req.session.data;
    if (req.session.breadcrumbs == undefined) {
        req.session.breadcrumbs = []
    }
    
    res.render(renderView, {
        breadcrumbs: req.session.breadcrumbs,
        session: req.session.data,
        resource: resourceUri,
        api: data,
        error: error,
        comment: comment,
        query: req.query,
        path: req.url,
        pathname: req.pathname,
        renderView: renderView,
        fatal: fatal,
        mode: process.env.NODE_ENV,
        pretty: true
    });
}

function toTitleCase(str) {
    return str.replace(/\w\S*/g, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
    });
}

function addBreadCrumb(breadcrumbs, path, breadcrumbName, icon, renderView, query, home) {
    if (home || breadcrumbName == "~") {
        while (breadcrumbs.length != 0) {
            breadcrumbs.pop()
        }
        if (breadcrumbName == "Home") {
            breadcrumbs.push({path: path, name: breadcrumbName});
        }
        return
    }
    if (breadcrumbName == "") {
        breadcrumbName = toTitleCase(renderView)
    }
    for (index in breadcrumbs) {
        var crumb = breadcrumbs[index];
        if (crumb.path == path) {
            do {
                var top = breadcrumbs.pop()
            } while (top.path != path);
            break;
        }
    }
    breadcrumbs.push({path: path, name: breadcrumbName, icon: icon});
}

function hasName(breadcrumbs, name) {
    var index;
    for (index in breadcrumbs) {
        var breadcrumb = breadcrumbs[index];
        if (breadcrumb.name == name) {
            return true
        }
    }
    return false
}

function checkAuth(req, res, next) {
    next();
    console.log('checkAuth ' + req.method + " " + req.url);
}

function missingPage(req, res, next) {
    console.log('Missing ' + req.method + " " + req.url);
    res.status(404);
    render('system/missing', req, res, undefined);
}

function errorDebug(err, req, res, next) {
    console.log('Error: ' + err.message);
    res.status(err.status || 500);
    render('system/nodeError', req, res, undefined, err);
}

module.exports = {
    add: add,
    router: router,
    getApiData: getApiData,
    apiRoot: apiRoot,
    addBreadCrumb: addBreadCrumb,
    checkAuth: checkAuth,
    missingPage: missingPage,
    errorDebug: errorDebug,
    hasName: hasName
};





