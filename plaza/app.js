/*
 * Copyright (c) Mike Brickman 2014-2017
 */

// ************ required modules ********************
//var Ddos = require('ddos');
var express = require('express');
var path = require('path');
var rfs = require('rotating-file-stream');
var favicon = require('serve-favicon');
var logger = require('morgan');
var fs = require('fs');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var session = require('express-session');
var flash = require('connect-flash');
var utils = require("./lib/utils.js");
var routes = require('./routes/routes.js');
var routing = require("./lib/routing.js");
var dateFormat = require("./lib/date_format.js");
var MySQLStore = require('express-mysql-session')(session);
//var ddos = new Ddos({burst:10, limit:15, testmode:false, whitelist:['138.201.171.149','136.243.148.93']});

logger.token('remote-addr', function(req) {
    return req.headers['x-forwarded-for'] || req.connection.remoteAddress
});

logger.token('sessionId', function(request) {
    return request.sessionID
});

var app = express();
app.set('trust proxy', true);

var options = {
    host: 'localhost',
    port: 3350,
    user: 'developer',
    password: 'tomato',
    database: 'session'
};

var sessionStore = new MySQLStore(options);
var maxAge = 7 * 24 * 60 * 60 * 1000; // 7 days

// ************ Settings ********************
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'pug');

// ************ Middleware etc ********************
//app.use(ddos.express);
var format = '":remote-addr", ":sessionId", ":date[clf]", ":method", ":url", ":http-version", :status, ":res[content-length]", ":referrer", ":user-agent"';
var combined = ':req[x-forwarded-for] - :remote-user [:date[clf]] ":method :url HTTP/:http-version" :status :res[content-length] ":referrer" ":user-agent"';
// create a rotating write stream
var accessLogStream = rfs('plaza.log', {
    interval: '1d', // rotate daily
    path: path.join('/data/logs', 'plaza')
});

app.use(logger('combined', { stream: accessLogStream }));
app.use(favicon(path.join(__dirname, 'public_static', '/img/favicon.ico')));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));
app.use(flash());
app.use(cookieParser());
app.use(session({secret: 'thewrongtrousers', store: sessionStore, resave: true, saveUninitialized: true, cookie: {maxAge: maxAge}}));
app.use(express.static(path.join(__dirname, 'public_static'), {immutable: true, maxage: '24h'}));
app.use(express.static(path.join(__dirname, 'public'), {immutable: true, maxage: '24h'}));
app.use(express.static(path.join(__dirname, 'other/magazine'), {immutable: true, maxage: '24h'}));
app.use(routing.checkAuth);
app.use('/', routing.router);
app.use(routing.missingPage);
app.use(routing.errorDebug);


// ************ System wide stuff ********************

routing.getApiData("/breed/list", 100000, function(apiData){
    app.locals.breeds=apiData.data.breeds;
});
routing.getApiData("/height/list", 100000, function(apiData){
    app.locals.allHeights=apiData.data.heights;
});
routing.getApiData("/height/list/kc", 100000, function(apiData){
    app.locals.kcHeights=apiData.data.heights;
});
routing.getApiData("/height/list/uka", 100000, function(apiData){
    app.locals.ukaHeights=apiData.data.heights;
});
routing.getApiData("/height/list/fab", 100000, function(apiData){
    app.locals.fabHeights=apiData.data.heights;
});
routing.getApiData("/height/list/ifcs", 100000, function(apiData){
    app.locals.ifcsHeights=apiData.data.heights;
});
routing.getApiData("/height/list/uka?casual", 100000, function(apiData){
    app.locals.ukaCasualHeights=apiData.data.heights;
});
routing.getApiData("/grade/list", 100000, function(apiData){
    app.locals.allGrades=apiData.data.grades;
});
routing.getApiData("/grade/list/kc", 100000, function(apiData){
    app.locals.kcGrades=apiData.data.grades;
});
routing.getApiData("/grade/list/uka", 100000, function(apiData){
    app.locals.ukaGrades=apiData.data.grades;
});
routing.getApiData("/grade/list/fab", 100000, function(apiData){
    app.locals.fabGrades=apiData.data.grades;
});
routing.getApiData("/country/list", 100000, function(apiData){
    app.locals.countries=apiData.data.countries;
});
routing.getApiData("/region/list", 100000, function(apiData){
    app.locals.regions=apiData.data.regions;
});
routing.getApiData("/height/list/op", 100000, function(apiData){
    app.locals.ukOpenHeights=apiData.data.heights;
});
routing.getApiData("/dogState/list", 100000, function(apiData){
    app.locals.dogStates=apiData.data.states;
});
routing.getApiData("/voucherType/list", 100000, function(apiData){
    app.locals.voucherTypes=apiData.data.voucherTypes;
});


app.locals.gender=[{value:1, description: "Dog"}, {value:2, description: "Bitch"}];
app.locals.kcWarrant=[{value:0, description: "None"}, {value:1, description: "Bronze"}, {value:2, description: "Silver"}, {value:3, description: "Gold"}, {value:4, description: "Platinum"}, {value:5, description: "Diamond"}];

app.locals.title = "Agility Plaza";
app.locals.dateRange = utils.dateRange;
app.locals.toLabel = utils.columnNameToLabel;
app.locals.beforeToday = utils.beforeToday;
app.locals.afterToday = utils.afterToday;
app.locals.isOrBeforeToday = utils.isOrBeforeToday;
app.locals.isToday = utils.isToday;
app.locals.toMoney = utils.toMoney;
app.locals.toJson = utils.toJson;
app.locals.codeToText = utils.codeToText;

app.locals.dateFormat = dateFormat.dateFormat;

app.locals.apiRoot = routing.apiRoot;
app.locals.addBreadCrumb = routing.addBreadCrumb;
app.locals.hasName = routing.hasName;

app.locals.calcHandlingFee = utils.calcHandlingFee;
app.locals.commaAppend = utils.commaAppend;

module.exports = app;

