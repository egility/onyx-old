/*
 * Copyright (c) Mike Brickman 2014-2017
 */

// #!/usr/bin/env node

var utils= require("./lib/utils");
utils.setSite();

var app = require('./app');
var debug = require('debug')('agilityplaza:server');
var http = require('http');
var https = require('https');
var fs = require('fs');


var port = utils.getConfig("port");
var ip = utils.getConfig("ip");
app.locals.bannerColor =  utils.getConfig("bannerColor");

app.set('port', port);
app.set('ip', ip);

var server = http.createServer(app);
server.listen(port, ip);
server.on('error', onError);
server.on('listening', onListening);

/*
console.log(__dirname);

var options = {
    key: fs.readFileSync(__dirname + '/ssl/privkey1.pem'),
    cert: fs.readFileSync(__dirname + '/ssl/cert1.pem')
};

var secure = https.createServer(options, app);
secure.listen(port, ip);
secure.on('error', onError);
secure.on('listening', onListening);
*/

function onError(error) {
    if (error.syscall !== 'listen') {
        throw error;
    }

    var bind = ip + ":" + port

    // handle specific listen errors with friendly messages
    switch (error.code) {
        case 'EACCES':
            console.error(bind + ' requires elevated privileges');
            process.exit(1);
            break;
        case 'EADDRINUSE':
            console.error(bind + ' is already in use');
            process.exit(1);
            break;
        default:
            throw error;
    }
}

function onListening() {
    console.log('Listening on ' + ip + ":" + port);
    console.log('Environment: ' + process.env.NODE_ENV)
}
